package com.example.smart.lighting.scenes.with_natural.language.service.mqtt;

import com.example.smart.lighting.scenes.with_natural.language.service.MqttService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes commands to MQTT topics for IoT device control.
 *

 */
@Component
@Slf4j
public class MqttCommandPublisher {

    @Value("${mqtt.topic.prefix}")
    private String topicPrefix;

    private final MqttService mqttService;

    public MqttCommandPublisher(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * Sends a command to control a specific LED.
     *
     * @param controllerId the ESP32 controller identifier
     * @param ledIndex the LED index (0-based)
     * @param command the LED command
     */
    public void sendLedCommand(String controllerId, int ledIndex, MqttService.LedCommand command) {
        String topic = String.format("%s/command/%s/led/%d", topicPrefix, controllerId, ledIndex);
        mqttService.publish(topic, command);
    }

    /**
     * Sends a scene activation command to a controller.
     *
     * @param controllerId the ESP32 controller identifier
     * @param sceneName the name of the scene to activate
     */
    public void sendSceneCommand(String controllerId, String sceneName) {
        String topic = String.format("%s/command/%s/scene", topicPrefix, controllerId);
        mqttService.publish(topic, new MqttService.SceneCommand(sceneName));
    }

    /**
     * Sends a global command affecting all LEDs on a controller.
     *
     * @param controllerId the ESP32 controller identifier
     * @param command the global command
     */
    public void sendGlobalCommand(String controllerId, MqttService.GlobalCommand command) {
        String topic = String.format("%s/command/%s/global", topicPrefix, controllerId);
        mqttService.publish(topic, command);
    }

    /**
     * Publishes a command to control a specific LED by index.
     * Topic: smartlighting/led/{index}/set
     *
     * @param ledIndex the LED index (0-based)
     * @param command the command map
     */
    public void publishLedCommand(int ledIndex, Map<String, Object> command) {
        String topic = String.format("%s/led/%d/set", topicPrefix, ledIndex);
        log.info("Publishing LED command to {}: {}", topic, command);
        mqttService.publish(topic, command);
    }

    /**
     * Publishes a scene command for a specific LED.
     * Topic: smartlighting/led/{index}/scene
     *
     * @param ledIndex the LED index (0-based)
     * @param sceneName the scene name
     */
    public void publishSceneCommand(int ledIndex, String sceneName) {
        String topic = String.format("%s/led/%d/scene", topicPrefix, ledIndex);
        log.info("Publishing scene command to {}: {}", topic, sceneName);
        mqttService.publish(topic, Map.of("scene", sceneName));
    }

    /**
     * Publishes a mode command for a controller.
     * Topic: smartlighting/mode/set
     *
     * @param controllerId the controller ID (unused, kept for API compatibility)
     * @param mode the mode (auto/manual)
     */
    public void publishModeCommand(String controllerId, String mode) {
        String topic = String.format("%s/mode/set", topicPrefix);
        log.info("Publishing global mode command to {}: {}", topic, mode);
        mqttService.publish(topic, mode);
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
        mqttService.publish(topic, Map.of("mode", mode));
    }
}

