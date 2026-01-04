package com.example.smart.lighting.scenes.with_natural.language.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for broadcasting events to WebSocket clients.
 *
 * <p>Provides methods to push real-time updates to connected frontend clients
 * using STOMP over WebSocket. Events are broadcast to topic channels that
 * clients subscribe to.</p>
 *
 * <h3>Topic Channels:</h3>
 * <ul>
 *   <li>{@code /topic/device-state} - Device state changes (on/off, brightness, color)</li>
 *   <li>{@code /topic/scenes} - Scene application status (pending, confirmed, timeout)</li>
 *   <li>{@code /topic/sensors} - Sensor data updates (temperature, humidity, etc.)</li>
 *   <li>{@code /topic/rules} - Automation rule triggers</li>
 *   <li>{@code /topic/system} - System-level events</li>
 * </ul>
 *

 * @see SimpMessagingTemplate
 * @see WebSocketMessage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts a device state change to all subscribed clients.
     *
     * @param deviceId the device UUID
     * @param state the new device state
     */
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

    /**
     * Broadcasts that a scene has been applied.
     *
     * @param sceneId the scene UUID
     * @param sceneName the scene name
     * @param devicesAffected number of devices affected
     */
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

    /**
     * Broadcast scene command pending (sent to MQTT, awaiting ESP32 ack).
     */
    public void broadcastScenePending(UUID sceneId, String sceneName, String correlationId, int lightsAffected) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SCENE_PENDING")
            .data(Map.of(
                "sceneId", sceneId != null ? sceneId : "nlp-command",
                "sceneName", sceneName,
                "correlationId", correlationId,
                "lightsAffected", lightsAffected
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/scenes", message);
        log.info("Broadcasted scene pending: {} (correlationId={})", sceneName, correlationId);
    }

    /**
     * Broadcast NLP command pending (sent to MQTT, awaiting ESP32 ack).
     */
    public void broadcastNlpPending(String commandName, UUID correlationId, int lightsAffected) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SCENE_PENDING")  // Reuse same type so frontend handles it consistently
            .data(Map.of(
                "sceneId", "nlp-command",
                "sceneName", commandName,
                "correlationId", correlationId.toString(),
                "lightsAffected", lightsAffected
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/scenes", message);
        log.info("Broadcasted NLP command pending: {} (correlationId={})", commandName, correlationId);
    }

    /**
     * Broadcast scene command confirmed (ESP32 acknowledged).
     */
    public void broadcastSceneConfirmed(UUID sceneId, String sceneName, String correlationId,
                                        int devicesConfirmed, long latencyMs) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SCENE_CONFIRMED")
            .data(Map.of(
                "sceneId", sceneId,
                "sceneName", sceneName,
                "correlationId", correlationId,
                "devicesConfirmed", devicesConfirmed,
                "latencyMs", latencyMs
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/scenes", message);
        log.info("Broadcasted scene confirmed: {} ({}ms)", sceneName, latencyMs);
    }

    /**
     * Broadcast scene command timeout (no ack received in time).
     */
    public void broadcastSceneTimeout(UUID sceneId, String sceneName, String correlationId,
                                     int acksReceived, int lightsExpected) {
        WebSocketMessage message = WebSocketMessage.builder()
            .type("SCENE_TIMEOUT")
            .data(Map.of(
                "sceneId", sceneId,
                "sceneName", sceneName,
                "correlationId", correlationId,
                "acksReceived", acksReceived,
                "lightsExpected", lightsExpected
            ))
            .timestamp(System.currentTimeMillis())
            .build();

        messagingTemplate.convertAndSend("/topic/scenes", message);
        log.warn("Broadcasted scene timeout: {} ({}/{} acks)", sceneName, acksReceived, lightsExpected);
    }

    /**
     * Broadcasts that an automation rule was triggered.
     *
     * @param ruleId the rule UUID
     * @param ruleName the rule name
     */
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

    /**
     * Broadcasts a system-level event.
     *
     * @param eventType the type of system event
     * @param eventData additional event data
     */
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
