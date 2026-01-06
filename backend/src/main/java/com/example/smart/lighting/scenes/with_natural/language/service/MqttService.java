package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.service.mqtt.MqttMessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Service for MQTT communication with IoT devices.
 *
 * <p>Handles publishing commands to ESP32 controllers and receiving
 * status updates and sensor data from connected devices.</p>
 *
 * <p>This service delegates incoming message handling to
 * {@link MqttMessageHandler} for processing.</p>
 *
 * <p>Topic structure follows the pattern:</p>
 * <ul>
 *   <li>{@code {prefix}/command/{controllerId}/led/{index}} - LED commands</li>
 *   <li>{@code {prefix}/command/{controllerId}/scene} - Scene commands</li>
 *   <li>{@code {prefix}/status/{controllerId}} - Status updates</li>
 *   <li>{@code {prefix}/sensor/{sensorId}} - Sensor data</li>
 * </ul>
 *

 */
@Slf4j
@Service
public class MqttService {

    @Value("${mqtt.topic.prefix}")
    private String topicPrefix;

    private final MessageChannel mqttOutputChannel;
    private final ObjectMapper objectMapper;
    private final MqttMessageHandler messageHandler;

    /**
     * Constructs the MQTT service with required dependencies.
     */
    public MqttService(@Qualifier("mqttOutputChannel") MessageChannel mqttOutputChannel,
                       ObjectMapper objectMapper,
                       MqttMessageHandler messageHandler) {
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
        this.messageHandler = messageHandler;
    }

    /**
     * Publishes a message to an MQTT topic.
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

        // Check if message is retained (stored on broker, not fresh from device)
        Boolean retained = (Boolean) message.getHeaders().get(MqttHeaders.RECEIVED_RETAINED);
        boolean isRetained = Boolean.TRUE.equals(retained);

        Object payloadObj = message.getPayload();
        String payload;
        if (payloadObj instanceof byte[]) {
            payload = new String((byte[]) payloadObj, StandardCharsets.UTF_8);
        } else {
            payload = payloadObj.toString();
        }

        messageHandler.handleMessage(topic, payload, isRetained);
    }

    /**
     * Sends a command to control a specific LED.
     *
     * @param controllerId the ESP32 controller identifier
     * @param ledIndex the LED index (0-based)
     * @param command the LED command
     */
    public void sendLedCommand(String controllerId, int ledIndex, LedCommand command) {
        String topic = String.format("%s/command/%s/led/%d", topicPrefix, controllerId, ledIndex);
        publish(topic, command);
    }

    /**
     * Sends a scene activation command to a controller.
     *
     * @param controllerId the ESP32 controller identifier
     * @param sceneName the name of the scene to activate
     */
    public void sendSceneCommand(String controllerId, String sceneName) {
        String topic = String.format("%s/command/%s/scene", topicPrefix, controllerId);
        publish(topic, new SceneCommand(sceneName));
    }

    /**
     * Sends a global command affecting all LEDs on a controller.
     *
     * @param controllerId the ESP32 controller identifier
     * @param command the global command
     */
    public void sendGlobalCommand(String controllerId, GlobalCommand command) {
        String topic = String.format("%s/command/%s/global", topicPrefix, controllerId);
        publish(topic, command);
    }

    /**
     * Publishes a command to control a specific LED by index.
     *
     * @param ledIndex the LED index (0-based)
     * @param command the command map
     */
    public void publishLedCommand(int ledIndex, Map<String, Object> command) {
        String topic = String.format("%s/led/%d/set", topicPrefix, ledIndex);
        log.info("Publishing LED command to {}: {}", topic, command);
        publish(topic, command);
    }

    /**
     * Publishes a scene command for a specific LED.
     *
     * @param ledIndex the LED index (0-based)
     * @param sceneName the scene name
     */
    public void publishSceneCommand(int ledIndex, String sceneName) {
        String topic = String.format("%s/led/%d/scene", topicPrefix, ledIndex);
        log.info("Publishing scene command to {}: {}", topic, sceneName);
        publish(topic, Map.of("scene", sceneName));
    }

    /**
     * Publishes a mode command for a controller.
     *
     * @param controllerId the controller ID
     * @param mode the mode (auto/manual)
     */
    public void publishModeCommand(String controllerId, String mode) {
        String topic = String.format("%s/mode/set", topicPrefix);
        log.info("Publishing global mode command to {}: {}", topic, mode);
        publish(topic, mode);
    }

    /**
     * Set mode for a specific LED.
     *
     * @param ledIndex the LED index
     * @param mode the mode
     */
    public void publishLedModeCommand(int ledIndex, String mode) {
        String topic = String.format("%s/led/%d/set", topicPrefix, ledIndex);
        log.info("Publishing LED {} mode command: {}", ledIndex, mode);
        publish(topic, Map.of("mode", mode));
    }

    /**
     * Command payload for controlling individual LEDs.
     *
     * @param rgb RGB color values as array [r, g, b] (0-255 each)
     * @param brightness brightness level (0-100)
     * @param on whether the LED should be on or off
     */
    public record LedCommand(int[] rgb, int brightness, boolean on) {}

    /**
     * Command payload for activating lighting scenes.
     *
     * @param sceneName the predefined scene name
     */
    public record SceneCommand(String sceneName) {}

    /**
     * Command payload for global lighting operations.
     *
     * @param action the action: "on", "off", or "brightness"
     * @param brightness optional brightness level (0-100)
     * @param mode operation mode: "auto" or "manual"
     */
    public record GlobalCommand(String action, Integer brightness, String mode) {}
}
