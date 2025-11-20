package com.example.smart.lighting.scenes.with_natural.language.service;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MqttService {

    private final MessageChannel mqttOutputChannel;
    private final ObjectMapper objectMapper;

    public MqttService(@Qualifier("mqttOutputChannel") MessageChannel mqttOutputChannel, 
                       ObjectMapper objectMapper) {
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
    }

    @Value("${mqtt.topic.prefix}")
    private String topicPrefix;

    /**
     * Publish a message to MQTT topic
     */
    public void publish(String topic, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Message<String> message = MessageBuilder
                .withPayload(jsonPayload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();
            
            mqttOutputChannel.send(message);
            log.debug("Published to {}: {}", topic, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Handle incoming MQTT messages
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleIncomingMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
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
     * Handle device status messages
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
     * Handle sensor data messages
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
     * Send command to control LED
     */
    public void sendLedCommand(String controllerId, int ledIndex, LedCommand command) {
        String topic = String.format("%s/command/%s/led/%d", topicPrefix, controllerId, ledIndex);
        publish(topic, command);
    }

    /**
     * Send scene command
     */
    public void sendSceneCommand(String controllerId, String sceneName) {
        String topic = String.format("%s/command/%s/scene", topicPrefix, controllerId);
        publish(topic, new SceneCommand(sceneName));
    }

    /**
     * Send global command (all LEDs)
     */
    public void sendGlobalCommand(String controllerId, GlobalCommand command) {
        String topic = String.format("%s/command/%s/global", topicPrefix, controllerId);
        publish(topic, command);
    }

    // Command DTOs
    public record LedCommand(
        int[] rgb,
        int brightness,
        boolean on
    ) {}

    public record SceneCommand(
        String sceneName
    ) {}

    public record GlobalCommand(
        String action, // "on", "off", "brightness"
        Integer brightness,
        String mode // "auto", "manual"
    ) {}
}

