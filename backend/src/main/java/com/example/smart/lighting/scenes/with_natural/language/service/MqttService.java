package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.entity.DeviceState;
import com.example.smart.lighting.scenes.with_natural.language.entity.SensorReading;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceStateRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.SensorReadingRepository;
import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Service for MQTT communication with IoT devices.
 *
 * <p>Handles publishing commands to ESP32 controllers and receiving
 * status updates and sensor data from connected devices.</p>
 *
 * <p>Topic structure follows the pattern:</p>
 * <ul>
 *   <li>{@code {prefix}/command/{controllerId}/led/{index}} - LED commands</li>
 *   <li>{@code {prefix}/command/{controllerId}/scene} - Scene commands</li>
 *   <li>{@code {prefix}/status/{controllerId}} - Status updates</li>
 *   <li>{@code {prefix}/sensor/{sensorId}} - Sensor data</li>
 * </ul>
 *
 * @author Smart Lighting Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class MqttService {

    @Value("${mqtt.topic.prefix}")
    private String topicPrefix;

    private final MessageChannel mqttOutputChannel;
    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final DeviceStateRepository deviceStateRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final WebSocketEventService webSocketEventService;
    
    // Lazy to avoid circular dependency (ConfigService uses MQTT channel)
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ConfigService configService;

    /**
     * Constructs the MQTT service with required dependencies.
     */
    public MqttService(@Qualifier("mqttOutputChannel") MessageChannel mqttOutputChannel,
                       ObjectMapper objectMapper,
                       DeviceRepository deviceRepository,
                       DeviceStateRepository deviceStateRepository,
                       SensorReadingRepository sensorReadingRepository,
                       WebSocketEventService webSocketEventService) {
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.deviceStateRepository = deviceStateRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.webSocketEventService = webSocketEventService;
    }

    /**
     * Publishes a message to an MQTT topic.
     *
     * <p>The payload is automatically serialized to JSON before sending.</p>
     *
     * @param topic the MQTT topic to publish to
     * @param payload the message payload (will be JSON-serialized)
     */
    public void publish(String topic, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Message<String> message = MessageBuilder
                .withPayload(jsonPayload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();

            boolean sent = mqttOutputChannel.send(message, 5000);
            if (sent) {
                log.debug("Published to {}: {}", topic, jsonPayload);
            } else {
                log.warn("MQTT message send timed out for topic: {}", topic);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Handles incoming MQTT messages from IoT devices.
     *
     * <p>Routes messages to appropriate handlers based on topic:</p>
     * <ul>
     *   <li>Status topics → {@link #handleStatusMessage}</li>
     *   <li>Sensor topics → {@link #handleSensorMessage}</li>
     * </ul>
     *
     * @param message the incoming Spring Integration message
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    @Transactional
    public void handleIncomingMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        if (topic == null) {
            log.warn("Received MQTT message without topic header");
            return;
        }

        // Convert payload from byte[] to String
        Object payloadObj = message.getPayload();
        String payload;
        if (payloadObj instanceof byte[]) {
            payload = new String((byte[]) payloadObj, StandardCharsets.UTF_8);
        } else {
            payload = payloadObj.toString();
        }

        log.debug("Received from {}: {}", topic, payload);

        // Route to appropriate handler based on topic
        if (topic.contains("/led/") && topic.endsWith("/state")) {
            // LED state update: smartlighting/led/{index}/state
            handleLedStateMessage(topic, payload);
        } else if (topic.contains("/status/")) {
            handleStatusMessage(topic, payload);
        } else if (topic.contains("/sensor/")) {
            handleSensorMessage(topic, payload);
        } else if (topic.endsWith("/config/request")) {
            // Config request from ESP32 - publish current config
            handleConfigRequest(payload);
        }
    }
    
    /**
     * Handle config request from ESP32 device.
     */
    private void handleConfigRequest(String payload) {
        log.info("Config request received from ESP32: {}", payload);
        // ConfigService will publish full config to MQTT
        if (configService != null) {
            configService.publishFullConfigUpdate();
        }
    }

    /**
     * Handle device status messages (LED state updates).
     */
    private void handleStatusMessage(String topic, String payload) {
        try {
            // Parse topics like: smartlighting/led/0/state or smartlighting/status/esp32-001
            String[] parts = topic.split("/");
            
            if (topic.contains("/led/") && topic.endsWith("/state")) {
                // LED state update: smartlighting/led/{index}/state
                handleLedStateUpdate(parts, payload);
            } else if (parts.length >= 3) {
                String controllerId = parts[2];
                log.info("Status update from {}: {}", controllerId, payload);
                // Broadcast to WebSocket clients
                webSocketEventService.broadcastDeviceUpdate(controllerId, payload);
            }
        } catch (Exception e) {
            log.error("Error processing status message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle LED state message from MQTT topic: smartlighting/led/{index}/state
     */
    private void handleLedStateMessage(String topic, String payload) {
        log.info("LED state received - topic: {}, payload: {}", topic, payload);
        String[] parts = topic.split("/");
        handleLedStateUpdate(parts, payload);
    }

    /**
     * Handle LED state update from MQTT.
     */
    private void handleLedStateUpdate(String[] topicParts, String payload) {
        try {
            // Parse LED index from topic: smartlighting/led/{index}/state
            int ledIndex = Integer.parseInt(topicParts[2]);
            
            // Parse payload JSON
            Map<String, Object> state = objectMapper.readValue(payload, new TypeReference<>() {});
            
            // Find device by LED index (from meta_json)
            Optional<Device> deviceOpt = deviceRepository.findByLedIndex(ledIndex);
            if (deviceOpt.isEmpty()) {
                log.debug("No device found for LED index {}", ledIndex);
                return;
            }
            
            Device device = deviceOpt.get();
            
            // Extract values from payload
            Boolean isOn = state.containsKey("on") ? (Boolean) state.get("on") : null;
            Integer brightness = state.containsKey("brightness") 
                ? ((Number) state.get("brightness")).intValue() 
                : null;
            Integer saturation = state.containsKey("saturation")
                ? ((Number) state.get("saturation")).intValue()
                : null;
            Integer colorTemp = state.containsKey("color_temp")
                ? ((Number) state.get("color_temp")).intValue()
                : null;
            String rgbColor = null;
            if (state.containsKey("rgb") || state.containsKey("color")) {
                Object colorVal = state.get("rgb") != null ? state.get("rgb") : state.get("color");
                if (colorVal instanceof String) {
                    rgbColor = (String) colorVal;
                } else if (colorVal instanceof java.util.List<?> rgbList && rgbList.size() >= 3) {
                    // Handle RGB array format [R, G, B]
                    int r = ((Number) rgbList.get(0)).intValue();
                    int g = ((Number) rgbList.get(1)).intValue();
                    int b = ((Number) rgbList.get(2)).intValue();
                    rgbColor = String.format("#%02X%02X%02X", r, g, b);
                }
            }
            LocalDateTime now = LocalDateTime.now();
            
            log.info("LED {} update: on={}, brightness={}, saturation={}, colorTemp={}K, rgb={}", 
                ledIndex, isOn, brightness, saturation, colorTemp, rgbColor);
            
            // Upsert to handle concurrency
            deviceStateRepository.upsertLedState(device.getId(), isOn, brightness, rgbColor, now);
            log.info("Saved LED {} state to DB: on={}, brightness={}, rgb={}", ledIndex, isOn, brightness, rgbColor);
            
            // Broadcast to WebSocket with normalized keys for frontend
            java.util.Map<String, Object> wsState = new java.util.HashMap<>(state);
            wsState.put("brightnessPct", brightness);  // Add frontend-expected key
            wsState.put("isOn", isOn);  // Add frontend-expected key
            wsState.put("rgbColor", rgbColor);  // Add frontend-expected key
            wsState.put("saturationPct", saturation);  // Saturation percentage (humidity-based)
            wsState.put("colorTempKelvin", colorTemp);  // Color temperature in Kelvin (temperature-based)
            wsState.put("lastSeen", now.toString());
            webSocketEventService.broadcastDeviceStateUpdate(device.getId().toString(), wsState);
            log.info("Broadcasted LED {} state via WebSocket", ledIndex);
            
        } catch (Exception e) {
            log.error("Error updating LED state: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle sensor data messages.
     */
    private void handleSensorMessage(String topic, String payload) {
        try {
            // Extract sensor ID from topic: smartlighting/sensor/{sensor_name}
            String[] parts = topic.split("/");
            if (parts.length < 3) {
                return;
            }
            
            String sensorName = parts[2];
            log.debug("Sensor data from {}: {}", sensorName, payload);
            
            // Parse sensor data JSON - handle both object and array formats
            Map<String, Object> sensorData;
            String trimmedPayload = payload.trim();
            if (trimmedPayload.startsWith("[")) {
                // Array format - try to parse as list of objects and take first one
                // or as key-value pairs [[key, value], ...]
                log.warn("Sensor payload is array format, attempting conversion: {}", trimmedPayload);
                Object parsed = objectMapper.readValue(trimmedPayload, Object.class);
                if (parsed instanceof java.util.List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Map) {
                        sensorData = (Map<String, Object>) first;
                    } else {
                        log.warn("Cannot parse array sensor data: {}", trimmedPayload);
                        return;
                    }
                } else {
                    log.warn("Empty or invalid array sensor data: {}", trimmedPayload);
                    return;
                }
            } else {
                sensorData = objectMapper.readValue(trimmedPayload, new TypeReference<>() {});
            }
            
            // Find device by sensor_id in meta_json
            Optional<Device> deviceOpt = deviceRepository.findBySensorId(sensorName);
            if (deviceOpt.isEmpty()) {
                log.debug("No device found for sensor {}", sensorName);
                return;
            }
            
            Device device = deviceOpt.get();
            LocalDateTime now = LocalDateTime.now();
            
            // Store each sensor metric as a separate reading
            storeSensorReading(device, sensorData, "temperature", "t", "°C", now);
            storeSensorReading(device, sensorData, "humidity", "h", "%", now);
            storeSensorReading(device, sensorData, "luminosity", "l", "lux", now);
            storeSensorReading(device, sensorData, "light", "l", "lux", now);
            storeSensorReading(device, sensorData, "pressure", "p", "hPa", now);
            storeSensorReading(device, sensorData, "audio", "a", "dB", now);
            
            // Update device last_seen via DeviceState - upsert to handle concurrency
            deviceStateRepository.upsertLastSeen(device.getId(), now);
            
            // Broadcast to WebSocket
            webSocketEventService.broadcastSensorUpdate(sensorName, sensorData);
            
        } catch (Exception e) {
            log.error("Error processing sensor message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Store a sensor reading if present in the data.
     */
    private void storeSensorReading(Device device, Map<String, Object> data, 
                                    String fullKey, String shortKey, String unit, LocalDateTime timestamp) {
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

    /**
     * Sends a command to control a specific LED.
     *
     * @param controllerId the ESP32 controller identifier
     * @param ledIndex the LED index (0-based, corresponds to room)
     * @param command the LED command with RGB, brightness, and on/off state
     */
    public void sendLedCommand(String controllerId, int ledIndex, LedCommand command) {
        String topic = String.format("%s/command/%s/led/%d", topicPrefix, controllerId, ledIndex);
        publish(topic, command);
    }

    /**
     * Sends a scene activation command to a controller.
     *
     * @param controllerId the ESP32 controller identifier
     * @param sceneName the name of the scene to activate (e.g., "evening", "movie")
     */
    public void sendSceneCommand(String controllerId, String sceneName) {
        String topic = String.format("%s/command/%s/scene", topicPrefix, controllerId);
        publish(topic, new SceneCommand(sceneName));
    }

    /**
     * Sends a global command affecting all LEDs on a controller.
     *
     * @param controllerId the ESP32 controller identifier
     * @param command the global command (on/off, brightness, mode)
     */
    public void sendGlobalCommand(String controllerId, GlobalCommand command) {
        String topic = String.format("%s/command/%s/global", topicPrefix, controllerId);
        publish(topic, command);
    }

    /**
     * Publishes a command to control a specific LED by index.
     * Topic: smartlighting/led/{index}/set
     *
     * @param ledIndex the LED index (0-based)
     * @param command the command map containing on, rgb, brightness, color_temp, mode
     */
    public void publishLedCommand(int ledIndex, java.util.Map<String, Object> command) {
        String topic = String.format("%s/led/%d/set", topicPrefix, ledIndex);
        log.info("Publishing LED command to {}: {}", topic, command);
        publish(topic, command);
    }

    /**
     * Publishes a scene command for a specific LED.
     * Topic: smartlighting/led/{index}/scene
     *
     * @param ledIndex the LED index (0-based)
     * @param sceneName the scene name to apply
     */
    public void publishSceneCommand(int ledIndex, String sceneName) {
        String topic = String.format("%s/led/%d/scene", topicPrefix, ledIndex);
        log.info("Publishing scene command to {}: {}", topic, sceneName);
        publish(topic, java.util.Map.of("scene", sceneName));
    }

    /**
     * Publishes a mode command for a controller.
     * Topic: smartlighting/command/{controllerId}/mode
     *
     * @param controllerId the controller ID
     * @param mode the mode (auto/manual)
     */
    public void publishModeCommand(String controllerId, String mode) {
        // Send to global mode topic
        String topic = String.format("%s/mode/set", topicPrefix);
        log.info("Publishing global mode command to {}: {}", topic, mode);
        publish(topic, mode);  // Simple string payload
    }
    
    /**
     * Set mode for a specific LED.
     */
    public void publishLedModeCommand(int ledIndex, String mode) {
        String topic = String.format("%s/led/%d/set", topicPrefix, ledIndex);
        log.info("Publishing LED {} mode command: {}", ledIndex, mode);
        publish(topic, java.util.Map.of("mode", mode));
    }

    /**
     * Command payload for controlling individual LEDs.
     *
     * @param rgb RGB color values as array [r, g, b] (0-255 each)
     * @param brightness brightness level (0-100)
     * @param on whether the LED should be on or off
     */
    public record LedCommand(
        int[] rgb,
        int brightness,
        boolean on
    ) { }

    /**
     * Command payload for activating lighting scenes.
     *
     * @param sceneName the predefined scene name
     */
    public record SceneCommand(
        String sceneName
    ) { }

    /**
     * Command payload for global lighting operations.
     *
     * @param action the action: "on", "off", or "brightness"
     * @param brightness optional brightness level (0-100)
     * @param mode operation mode: "auto" or "manual"
     */
    public record GlobalCommand(
        String action,
        Integer brightness,
        String mode
    ) { }
}
