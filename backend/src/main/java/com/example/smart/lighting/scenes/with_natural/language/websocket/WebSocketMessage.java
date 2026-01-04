package com.example.smart.lighting.scenes.with_natural.language.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Data transfer object for WebSocket messages.
 *
 * <p>Standardized message format for all WebSocket communications.
 * Contains type discriminator, relevant IDs, and payload data.</p>
 *
 * <h3>Message Types:</h3>
 * <ul>
 *   <li>DEVICE_STATE_CHANGE - Device state update</li>
 *   <li>DEVICE_STATE_UPDATE - Device state from MQTT</li>
 *   <li>SCENE_PENDING - Scene command sent, awaiting ACK</li>
 *   <li>SCENE_CONFIRMED - Scene command acknowledged</li>
 *   <li>SCENE_TIMEOUT - Scene command timed out</li>
 *   <li>SENSOR_UPDATE - Sensor data received</li>
 *   <li>RULE_TRIGGERED - Automation rule executed</li>
 *   <li>SYSTEM_EVENT - System-level notification</li>
 * </ul>
 *

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    /** Message type discriminator. */
    private String type;

    /** Device ID (for device-related messages). */
    private UUID deviceId;

    /** Scene ID (for scene-related messages). */
    private UUID sceneId;

    /** Rule ID (for automation-related messages). */
    private UUID ruleId;

    /** Message payload data. */
    private Map<String, Object> data;

    /** Unix timestamp in milliseconds. */
    private Long timestamp;
}
