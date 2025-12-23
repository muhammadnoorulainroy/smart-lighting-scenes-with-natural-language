package com.example.smart.lighting.scenes.with_natural.language.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastDeviceStateChange(UUID deviceId, Map<String, Object> state) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("DEVICE_STATE_CHANGE")
            .deviceId(deviceId)
            .data(state)
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/device-state", message);
        log.debug("Broadcasted device state change for device: {}", deviceId);
    }

    public void broadcastSceneApplied(UUID sceneId, String sceneName, int devicesAffected) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SCENE_APPLIED")
            .data(Map.of(
                "sceneId", sceneId,
                "sceneName", sceneName,
                "devicesAffected", devicesAffected
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/scenes", message);
        log.info("Broadcasted scene applied: {}", sceneName);
    }

    public void broadcastRuleTriggered(UUID ruleId, String ruleName) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("RULE_TRIGGERED")
            .data(Map.of(
                "ruleId", ruleId,
                "ruleName", ruleName
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/rules", message);
        log.info("Broadcasted rule triggered: {}", ruleName);
    }

    public void broadcastSystemEvent(String eventType, Map<String, Object> eventData) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SYSTEM_EVENT")
            .data(Map.of(
                "eventType", eventType,
                "eventData", eventData
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/system", message);
        log.debug("Broadcasted system event: {}", eventType);
    }

    /**
     * Broadcast device update from MQTT.
     */
    public void broadcastDeviceUpdate(String controllerId, String payload) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("DEVICE_UPDATE")
            .data(Map.of(
                "controllerId", controllerId,
                "payload", payload
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/device-updates", message);
        log.debug("Broadcasted device update from controller: {}", controllerId);
    }

    /**
     * Broadcast device state update from MQTT.
     */
    public void broadcastDeviceStateUpdate(String deviceId, Map<String, Object> state) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("DEVICE_STATE_UPDATE")
            .data(Map.of(
                "deviceId", deviceId,
                "state", state
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/device-state", message);
        log.debug("Broadcasted device state update: {}", deviceId);
    }

    /**
     * Broadcast sensor data update from MQTT.
     */
    public void broadcastSensorUpdate(String sensorName, Map<String, Object> data) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SENSOR_UPDATE")
            .data(Map.of(
                "sensorName", sensorName,
                "readings", data
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/sensors", message);
        log.debug("Broadcasted sensor update: {}", sensorName);
    }
}
