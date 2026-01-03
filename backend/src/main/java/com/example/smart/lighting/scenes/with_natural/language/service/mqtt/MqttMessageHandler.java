package com.example.smart.lighting.scenes.with_natural.language.service.mqtt;

import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.entity.SensorReading;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceStateRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.SensorReadingRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.ConfigService;
import com.example.smart.lighting.scenes.with_natural.language.service.SceneCommandTracker;
import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles incoming MQTT messages and routes them to appropriate processors.
 *

 */
@Component
@Slf4j
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final DeviceStateRepository deviceStateRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final WebSocketEventService webSocketEventService;

    @Lazy
    @Autowired
    private ConfigService configService;

    @Lazy
    @Autowired
    private SceneCommandTracker sceneCommandTracker;

    public MqttMessageHandler(ObjectMapper objectMapper,
                              DeviceRepository deviceRepository,
                              DeviceStateRepository deviceStateRepository,
                              SensorReadingRepository sensorReadingRepository,
                              WebSocketEventService webSocketEventService) {
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.deviceStateRepository = deviceStateRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.webSocketEventService = webSocketEventService;
    }

    /**
     * Route a message to appropriate handler based on topic.
     *
     * @param topic the MQTT topic
     * @param payload the message payload
     */
    public void handleMessage(String topic, String payload) {
        log.debug("Received from {}: {}", topic, payload);

        if (topic.contains("/led/") && topic.endsWith("/state")) {
            handleLedStateMessage(topic, payload);
        } else if (topic.contains("/status/")) {
            handleStatusMessage(topic, payload);
        } else if (topic.contains("/sensor/")) {
            handleSensorMessage(topic, payload);
        } else if (topic.contains("/ack/")) {
            handleAckMessage(topic, payload);
        } else if (topic.endsWith("/config/request")) {
            handleConfigRequest(payload);
        }
    }

    /**
     * Handle acknowledgment messages from ESP32.
     */
    private void handleAckMessage(String topic, String payload) {
        try {
            log.info("Received ack from topic {}: {}", topic, payload);

            Map<String, Object> ackData = objectMapper.readValue(payload, new TypeReference<>() {});

            if (topic.contains("/ack/scene/")) {
                String correlationId = ackData.get("correlationId") != null
                    ? ackData.get("correlationId").toString()
                    : extractCorrelationIdFromTopic(topic);

                boolean success = !ackData.containsKey("success") || (Boolean) ackData.get("success");

                int ledIndex = ackData.containsKey("ledIndex")
                    ? ((Number) ackData.get("ledIndex")).intValue()
                    : -1;

                if (correlationId != null && sceneCommandTracker != null) {
                    sceneCommandTracker.processAck(correlationId, success, ledIndex);
                }
            }
        } catch (Exception e) {
            log.error("Error processing ack message: {}", e.getMessage(), e);
        }
    }

    private String extractCorrelationIdFromTopic(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 4 && "scene".equals(parts[2])) {
            return parts[3];
        }
        return null;
    }

    private void handleConfigRequest(String payload) {
        log.info("Config request received from ESP32: {}", payload);
        if (configService != null) {
            configService.publishFullConfigUpdate();
        }
    }

    private void handleStatusMessage(String topic, String payload) {
        try {
            String[] parts = topic.split("/");

            if (topic.contains("/led/") && topic.endsWith("/state")) {
                handleLedStateUpdate(parts, payload);
            } else if (parts.length >= 3) {
                String controllerId = parts[2];
                log.info("Status update from {}: {}", controllerId, payload);
                webSocketEventService.broadcastDeviceUpdate(controllerId, payload);
            }
        } catch (Exception e) {
            log.error("Error processing status message: {}", e.getMessage(), e);
        }
    }

    private void handleLedStateMessage(String topic, String payload) {
        log.info("LED state received - topic: {}, payload: {}", topic, payload);
        String[] parts = topic.split("/");
        handleLedStateUpdate(parts, payload);
    }

    @SuppressWarnings("unchecked")
    private void handleLedStateUpdate(String[] topicParts, String payload) {
        try {
            int ledIndex = Integer.parseInt(topicParts[2]);

            Map<String, Object> state = objectMapper.readValue(payload, new TypeReference<>() {});

            Optional<Device> deviceOpt = deviceRepository.findByLedIndex(ledIndex);
            if (deviceOpt.isEmpty()) {
                log.debug("No device found for LED index {}", ledIndex);
                return;
            }

            Device device = deviceOpt.get();

            Boolean isOn = state.containsKey("on") ? (Boolean) state.get("on") : null;
            Integer brightness = state.containsKey("brightness")
                ? ((Number) state.get("brightness")).intValue() : null;
            Integer saturation = state.containsKey("saturation")
                ? ((Number) state.get("saturation")).intValue() : null;
            Integer colorTemp = state.containsKey("color_temp")
                ? ((Number) state.get("color_temp")).intValue() : null;
            String rgbColor = extractRgbColor(state);
            LocalDateTime now = LocalDateTime.now();

            log.info("LED {} update: on={}, brightness={}, saturation={}, colorTemp={}K, rgb={}",
                ledIndex, isOn, brightness, saturation, colorTemp, rgbColor);

            deviceStateRepository.upsertLedState(device.getId(), isOn, brightness, rgbColor, now);
            log.info("Saved LED {} state to DB: on={}, brightness={}, rgb={}",
                ledIndex, isOn, brightness, rgbColor);

            Map<String, Object> wsState = new HashMap<>(state);
            wsState.put("brightnessPct", brightness);
            wsState.put("isOn", isOn);
            wsState.put("rgbColor", rgbColor);
            wsState.put("saturationPct", saturation);
            wsState.put("colorTempKelvin", colorTemp);
            wsState.put("lastSeen", now.toString());
            webSocketEventService.broadcastDeviceStateUpdate(device.getId().toString(), wsState);
            log.info("Broadcasted LED {} state via WebSocket", ledIndex);

        } catch (Exception e) {
            log.error("Error updating LED state: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractRgbColor(Map<String, Object> state) {
        if (!state.containsKey("rgb") && !state.containsKey("color")) {
            return null;
        }

        Object colorVal = state.get("rgb") != null ? state.get("rgb") : state.get("color");
        if (colorVal instanceof String) {
            return (String) colorVal;
        } else if (colorVal instanceof List<?> rgbList && rgbList.size() >= 3) {
            int r = ((Number) rgbList.get(0)).intValue();
            int g = ((Number) rgbList.get(1)).intValue();
            int b = ((Number) rgbList.get(2)).intValue();
            return String.format("#%02X%02X%02X", r, g, b);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void handleSensorMessage(String topic, String payload) {
        try {
            String[] parts = topic.split("/");
            if (parts.length < 3) {
                return;
            }

            String sensorName = parts[2];
            log.debug("Sensor data from {}: {}", sensorName, payload);

            Map<String, Object> sensorData = parseSensorPayload(payload);
            if (sensorData == null) {
                return;
            }

            Optional<Device> deviceOpt = deviceRepository.findBySensorId(sensorName);
            if (deviceOpt.isEmpty()) {
                log.debug("No device found for sensor {}", sensorName);
                return;
            }

            Device device = deviceOpt.get();
            LocalDateTime now = LocalDateTime.now();

            storeSensorReading(device, sensorData, "temperature", "t", "°C", now);
            storeSensorReading(device, sensorData, "humidity", "h", "%", now);
            storeSensorReading(device, sensorData, "luminosity", "l", "lux", now);
            storeSensorReading(device, sensorData, "light", "l", "lux", now);
            storeSensorReading(device, sensorData, "pressure", "p", "hPa", now);
            storeSensorReading(device, sensorData, "audio", "a", "dB", now);

            deviceStateRepository.upsertLastSeen(device.getId(), now);
            webSocketEventService.broadcastSensorUpdate(sensorName, sensorData);

        } catch (Exception e) {
            log.error("Error processing sensor message: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSensorPayload(String payload) {
        try {
            String trimmedPayload = payload.trim();
            if (trimmedPayload.startsWith("[")) {
                log.warn("Sensor payload is array format, attempting conversion: {}", trimmedPayload);
                Object parsed = objectMapper.readValue(trimmedPayload, Object.class);
                if (parsed instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Map) {
                        return (Map<String, Object>) first;
                    }
                }
                log.warn("Cannot parse array sensor data: {}", trimmedPayload);
                return null;
            }
            return objectMapper.readValue(trimmedPayload, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Error parsing sensor payload: {}", e.getMessage());
            return null;
        }
    }

    private void storeSensorReading(Device device, Map<String, Object> data,
                                    String fullKey, String shortKey, String unit,
                                    LocalDateTime timestamp) {
        Object value = data.get(fullKey);
        if (value == null) {
            value = data.get(shortKey);
        }
        if (value == null) {
            return;
        }

        try {
            BigDecimal numericValue = new BigDecimal(value.toString());
            SensorReading reading = SensorReading.builder()
                .device(device)
                .metric(fullKey)
                .value(numericValue)
                .unit(unit)
                .timestamp(timestamp)
                .build();
            sensorReadingRepository.save(reading);
        } catch (NumberFormatException e) {
            log.debug("Non-numeric sensor value for {}: {}", fullKey, value);
        }
    }
}

