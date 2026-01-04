package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.SystemConfig;
import com.example.smart.lighting.scenes.with_natural.language.repository.SystemConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing runtime system configuration.
 *
 * <p>Handles CRUD operations for configuration settings and publishes
 * updates to ESP32 devices via MQTT. Configuration is persisted in
 * PostgreSQL and includes default values for all categories.</p>
 *
 * <h3>Configuration Categories:</h3>
 * <ul>
 *   <li><b>lighting</b> - Brightness limits, auto-dim, sensor override</li>
 *   <li><b>climate</b> - Temperature/humidity color blending</li>
 *   <li><b>audio</b> - Disco mode and audio detection</li>
 *   <li><b>display</b> - OLED timeout and content settings</li>
 *   <li><b>mqtt</b> - Publish intervals and heartbeat</li>
 * </ul>
 *

 * @see SystemConfig
 */
@Service
@Slf4j
public class ConfigService {

    private final SystemConfigRepository configRepository;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    public ConfigService(
            SystemConfigRepository configRepository,
            ObjectMapper objectMapper,
            @Qualifier("mqttOutputChannel") MessageChannel mqttOutboundChannel) {
        this.configRepository = configRepository;
        this.objectMapper = objectMapper;
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    @Value("${mqtt.topic.prefix:smartlighting}")
    private String topicPrefix;

    /**
     * Initialize default configs if not present.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaults() {
        log.info("Initializing system configuration defaults...");

        createIfNotExists("lighting", "Lighting mode and brightness settings", SystemConfig.Defaults.lighting());
        createIfNotExists("climate", "Temperature and humidity color adjustments", SystemConfig.Defaults.climate());
        createIfNotExists("audio", "Audio detection and disco mode settings", SystemConfig.Defaults.audio());
        createIfNotExists("display", "OLED display settings", SystemConfig.Defaults.display());
        createIfNotExists("mqtt", "MQTT communication settings", SystemConfig.Defaults.mqtt());

        log.info("System configuration initialized");
    }

    private void createIfNotExists(String key, String description, Map<String, Object> defaults) {
        if (!configRepository.existsById(key)) {
            SystemConfig config = SystemConfig.builder()
                .key(key)
                .description(description)
                .settings(defaults)
                .build();
            configRepository.save(config);
            log.info("Created default config: {}", key);
        }
    }

    /**
     * Get all configuration categories.
     */
    public Map<String, Object> getAllConfig() {
        Map<String, Object> allConfig = new HashMap<>();
        List<SystemConfig> configs = configRepository.findAllByOrderByKeyAsc();

        for (SystemConfig config : configs) {
            allConfig.put(config.getKey(), config.getSettings());
        }

        return allConfig;
    }

    /**
     * Get configuration for a specific category.
     */
    public Optional<Map<String, Object>> getConfig(String key) {
        return configRepository.findById(key)
            .map(SystemConfig::getSettings);
    }

    /**
     * Update configuration for a category.
     * Publishes changes to MQTT for ESP32 devices.
     */
    public Map<String, Object> updateConfig(String key, Map<String, Object> updates, String updatedBy) {
        SystemConfig config = configRepository.findById(key)
            .orElseGet(() -> SystemConfig.builder()
                .key(key)
                .settings(new HashMap<>())
                .build());

        // Merge updates with existing settings
        Map<String, Object> settings = new HashMap<>(config.getSettings());
        settings.putAll(updates);
        config.setSettings(settings);
        config.setUpdatedBy(updatedBy);

        configRepository.save(config);
        log.info("Updated config '{}' by {}: {}", key, updatedBy, updates.keySet());

        // Publish to MQTT
        publishConfigUpdate(key, settings);

        return settings;
    }

    /**
     * Update multiple configuration categories at once.
     */
    public Map<String, Object> updateAllConfig(Map<String, Map<String, Object>> allUpdates, String updatedBy) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : allUpdates.entrySet()) {
            Map<String, Object> updated = updateConfig(entry.getKey(), entry.getValue(), updatedBy);
            result.put(entry.getKey(), updated);
        }

        // Publish full config update
        publishFullConfigUpdate();

        return result;
    }

    /**
     * Reset a category to defaults.
     */
    public Map<String, Object> resetToDefaults(String key) {
        Map<String, Object> defaults = getDefaultsForKey(key);
        if (defaults != null) {
            return updateConfig(key, defaults, "system");
        }
        return Map.of();
    }

    /**
     * Reset all categories to defaults.
     */
    public Map<String, Object> resetAllToDefaults() {
        Map<String, Object> result = new HashMap<>();
        result.put("lighting", updateConfig("lighting", SystemConfig.Defaults.lighting(), "system"));
        result.put("climate", updateConfig("climate", SystemConfig.Defaults.climate(), "system"));
        result.put("audio", updateConfig("audio", SystemConfig.Defaults.audio(), "system"));
        result.put("display", updateConfig("display", SystemConfig.Defaults.display(), "system"));
        result.put("mqtt", updateConfig("mqtt", SystemConfig.Defaults.mqtt(), "system"));

        publishFullConfigUpdate();

        return result;
    }

    private Map<String, Object> getDefaultsForKey(String key) {
        return switch (key) {
            case "lighting" -> SystemConfig.Defaults.lighting();
            case "climate" -> SystemConfig.Defaults.climate();
            case "audio" -> SystemConfig.Defaults.audio();
            case "display" -> SystemConfig.Defaults.display();
            case "mqtt" -> SystemConfig.Defaults.mqtt();
            default -> null;
        };
    }

    /**
     * Publish config update to MQTT for a specific category.
     */
    private void publishConfigUpdate(String category, Map<String, Object> settings) {
        try {
            String topic = topicPrefix + "/config/" + category;
            String payload = objectMapper.writeValueAsString(settings);

            mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build()
            );

            log.debug("Published config update to {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish config update: {}", e.getMessage());
        }
    }

    /**
     * Publish full configuration to MQTT.
     */
    public void publishFullConfigUpdate() {
        try {
            Map<String, Object> allConfig = getAllConfig();
            String topic = topicPrefix + "/config/update";
            String payload = objectMapper.writeValueAsString(allConfig);

            mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build()
            );

            log.info("Published full config update to {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish full config update: {}", e.getMessage());
        }
    }

    /**
     * Handle config request from ESP32.
     * Called when ESP32 publishes to smartlighting/config/request
     */
    public void handleConfigRequest(String clientId) {
        log.info("Config request received from: {}", clientId);
        publishFullConfigUpdate();
    }
}
