package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.Automation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing and executing Home Assistant-style automations.
 *
 * <p>Loads automation rules from YAML files and executes them based on
 * triggers and conditions. Supports light control and scene activation.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>YAML-based automation configuration</li>
 *   <li>Hot-reload of automation files</li>
 *   <li>Trigger, condition, and action evaluation</li>
 *   <li>Integration with MQTT for device control</li>
 * </ul>
 *

 * @see Automation
 * @see MqttService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final MqttService mqttService;
    private final ResourceLoader resourceLoader;

    @Value("${rules.file-path}")
    private String rulesFilePath;

    @Value("${rules.enabled}")
    private boolean rulesEnabled;

    private final Map<String, Automation> automations = new ConcurrentHashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @PostConstruct
    public void init() {
        if (rulesEnabled) {
            loadAutomations();
        }
    }

    /**
     * Load automations from YAML file.
     */
    @SuppressWarnings("unchecked")
    public void loadAutomations() {
        try {
            Resource resource = resourceLoader.getResource(rulesFilePath);
            if (resource.exists()) {
                List<Map<String, Object>> yamlList = yamlMapper.readValue(
                    resource.getInputStream(),
                    List.class
                );

                for (Map<String, Object> yamlMap : yamlList) {
                    Automation automation = yamlMapper.convertValue(yamlMap, Automation.class);
                    automations.put(automation.getId(), automation);
                    log.info("Loaded automation: {} ({})", automation.getId(), automation.getAlias());
                }

                log.info("Loaded {} automations from {}", automations.size(), rulesFilePath);
            } else {
                log.warn("Automation file not found: {}", rulesFilePath);
            }
        } catch (IOException e) {
            log.error("Failed to load automations: {}", e.getMessage(), e);
        }
    }

    /**
     * Reload automations from file.
     */
    public void reloadAutomations() {
        automations.clear();
        loadAutomations();
    }

    /**
     * Get all automations.
     */
    public List<Automation> getAllAutomations() {
        return new ArrayList<>(automations.values());
    }

    /**
     * Get automation by ID.
     */
    public Automation getAutomation(String id) {
        return automations.get(id);
    }

    /**
     * Execute automation action.
     */
    public void executeAutomation(String automationId) {
        Automation automation = automations.get(automationId);
        if (automation == null) {
            log.warn("Automation not found: {}", automationId);
            return;
        }

        if (!automation.isEnabled()) {
            log.info("Automation {} is disabled, skipping", automationId);
            return;
        }

        log.info("Executing automation: {} ({})", automationId, automation.getAlias());

        for (Automation.Action action : automation.getActions()) {
            executeAction(action);
        }
    }

    /**
     * Execute a single action.
     */
    private void executeAction(Automation.Action action) {
        String service = action.getAction() != null ? action.getAction() : action.getService();

        if (service == null) {
            log.warn("Action has no service defined");
            return;
        }

        try {
            // Parse service call: domain.service (e.g., light.turn_on)
            String[] parts = service.split("\\.");
            if (parts.length != 2) {
                log.warn("Invalid service format: {}", service);
                return;
            }

            String domain = parts[0];
            String serviceName = parts[1];

            // Handle light domain
            if ("light".equals(domain)) {
                handleLightService(serviceName, action.getTarget(), action.getData());
            } else if ("scene".equals(domain)) {
                handleSceneService(serviceName, action.getTarget());
            } else {
                log.warn("Unknown service domain: {}", domain);
            }
        } catch (Exception e) {
            log.error("Error executing action: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle light service calls.
     */
    private void handleLightService(String service, Map<String, Object> target, Map<String, Object> data) {
        // Extract controller ID from target if provided, otherwise use default
        String controllerId = "esp32-001";
        if (target != null && target.containsKey("controller_id")) {
            controllerId = (String) target.get("controller_id");
        }

        switch (service) {
            case "turn_on":
                int[] rgb = data != null && data.containsKey("rgb_color")
                    ? parseRgbColor(data.get("rgb_color"))
                    : new int[]{255, 255, 255};

                int brightness = data != null && data.containsKey("brightness")
                    ? ((Number) data.get("brightness")).intValue()
                    : 100;

                mqttService.sendLedCommand(controllerId, 0, new MqttService.LedCommand(rgb, brightness, true));
                log.info("Sent turn_on command: RGB={}, Brightness={}", rgb, brightness);
                break;

            case "turn_off":
                mqttService.sendGlobalCommand(controllerId, new MqttService.GlobalCommand("off", null, null));
                log.info("Sent turn_off command");
                break;

            default:
                log.warn("Unknown light service: {}", service);
        }
    }

    /**
     * Handle scene service calls.
     */
    private void handleSceneService(String service, Map<String, Object> target) {
        if ("turn_on".equals(service) && target != null) {
            String sceneName = (String) target.get("entity_id");
            if (sceneName != null) {
                sceneName = sceneName.replace("scene.", "");
                mqttService.sendSceneCommand("esp32-001", sceneName);
                log.info("Activated scene: {}", sceneName);
            }
        }
    }

    /**
     * Parse RGB color from various formats.
     */
    private int[] parseRgbColor(Object colorObj) {
        if (colorObj instanceof List) {
            List<?> list = (List<?>) colorObj;
            return new int[]{
                ((Number) list.get(0)).intValue(),
                ((Number) list.get(1)).intValue(),
                ((Number) list.get(2)).intValue()
            };
        }
        return new int[]{255, 255, 255};
    }

    /**
     * Check if a trigger condition is met.
     */
    public boolean evaluateTrigger(Automation.Trigger trigger, Map<String, Object> context) {
        // TODO: Implement trigger evaluation based on type
        log.debug("Evaluating trigger: {}", trigger);
        return false;
    }

    /**
     * Check if all conditions are met.
     */
    public boolean evaluateConditions(List<Automation.Condition> conditions, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (Automation.Condition condition : conditions) {
            if (!evaluateCondition(condition)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate a single condition.
     */
    private boolean evaluateCondition(Automation.Condition condition) {
        // TODO: Implement condition evaluation based on type
        log.debug("Evaluating condition: {}", condition);
        return true;
    }
}
