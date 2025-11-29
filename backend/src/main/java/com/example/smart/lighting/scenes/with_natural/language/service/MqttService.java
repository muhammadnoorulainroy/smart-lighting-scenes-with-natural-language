package com.example.smart.lighting.scenes.with_natural.language.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

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

    /**
     * Constructs the MQTT service with required dependencies.
     *
     * @param mqttOutputChannel the Spring Integration channel for outbound messages
     * @param objectMapper Jackson mapper for JSON serialization
     */
    public MqttService(@Qualifier("mqttOutputChannel") MessageChannel mqttOutputChannel,
                       ObjectMapper objectMapper) {
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
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
    public void handleIncomingMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        if (topic == null) {
            log.warn("Received MQTT message without topic header");
            return;
        }

        String payload = message.getPayload().toString();

        log.debug("Received from {}: {}", topic, payload);

        // Route to appropriate handler based on topic
        if (topic.contains("/status/")) {
            handleStatusMessage(topic, payload);
        } else if (topic.contains("/sensor/")) {
            handleSensorMessage(topic, payload);
        }
    }

    /**
     * Handle device status messages.
     */
    private void handleStatusMessage(String topic, String payload) {
        try {
            // Extract controller ID from topic: smartlighting/status/esp32-001
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                String controllerId = parts[2];
                log.info("Status update from {}: {}", controllerId, payload);
                // TODO: Update device state in database
            }
        } catch (Exception e) {
            log.error("Error processing status message: {}", e.getMessage());
        }
    }

    /**
     * Handle sensor data messages.
     */
    private void handleSensorMessage(String topic, String payload) {
        try {
            // Extract sensor ID from topic: smartlighting/sensor/sensor_1
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                String sensorId = parts[2];
                log.info("Sensor data from {}: {}", sensorId, payload);
                // TODO: Process sensor data and trigger automations
            }
        } catch (Exception e) {
            log.error("Error processing sensor message: {}", e.getMessage());
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
