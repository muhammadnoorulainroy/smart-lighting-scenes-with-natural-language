package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending scene commands and their acknowledgments from ESP32 devices.
 *
 * <p>Implements an end-to-end acknowledgment system for MQTT commands:</p>
 * <ol>
 *   <li>Backend sends command with correlation ID to ESP32</li>
 *   <li>ESP32 processes command and sends ACK back</li>
 *   <li>This service correlates ACKs and broadcasts status via WebSocket</li>
 * </ol>
 *
 * <h3>Status Flow:</h3>
 * <ul>
 *   <li>PENDING - Command sent, awaiting acknowledgment</li>
 *   <li>CONFIRMED - All expected ACKs received</li>
 *   <li>TIMEOUT - ACKs not received within timeout period</li>
 * </ul>
 *

 * @see WebSocketEventService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SceneCommandTracker {

    private static final long COMMAND_TIMEOUT_MS = 10000; // 10 seconds timeout

    private final WebSocketEventService webSocketEventService;

    // Track pending commands: correlationId -> PendingCommand
    private final Map<String, PendingCommand> pendingCommands = new ConcurrentHashMap<>();

    /**
     * Register a scene command as pending.
     * @return correlation ID for tracking
     */
    public String registerCommand(UUID sceneId, String sceneName, int lightsAffected) {
        String correlationId = UUID.randomUUID().toString();

        PendingCommand pending = new PendingCommand(
            correlationId,
            sceneId,
            sceneName,
            lightsAffected,
            Instant.now()
        );

        pendingCommands.put(correlationId, pending);
        log.info("Registered pending scene command: {} for scene '{}'", correlationId, sceneName);

        // Broadcast pending status via WebSocket
        webSocketEventService.broadcastScenePending(sceneId, sceneName, correlationId, lightsAffected);

        return correlationId;
    }

    /**
     * Process an acknowledgment from ESP32.
     * @param correlationId the correlation ID from the ack message
     * @param success whether the ESP32 successfully applied the scene
     * @param ledIndex the LED index that acknowledged
     */
    public void processAck(String correlationId, boolean success, int ledIndex) {
        PendingCommand pending = pendingCommands.get(correlationId);

        if (pending == null) {
            log.warn("Received ack for unknown correlation ID: {}", correlationId);
            return;
        }

        pending.incrementAckCount();
        log.info("Received ack {}/{} for scene '{}' (LED {})",
            pending.getAckCount(), pending.getLightsAffected(),
            pending.getSceneName(), ledIndex);

        // Check if all expected acks received
        if (pending.getAckCount() >= pending.getLightsAffected()) {
            pendingCommands.remove(correlationId);
            long latencyMs = Instant.now().toEpochMilli() - pending.getCreatedAt().toEpochMilli();

            log.info("Scene '{}' confirmed by all {} devices in {}ms",
                pending.getSceneName(), pending.getLightsAffected(), latencyMs);

            // Broadcast confirmed status via WebSocket
            webSocketEventService.broadcastSceneConfirmed(
                pending.getSceneId(),
                pending.getSceneName(),
                correlationId,
                pending.getLightsAffected(),
                latencyMs
            );
        }
    }

    /**
     * Check for timed-out commands and broadcast timeout events.
     */
    @Scheduled(fixedRate = 2000) // Check every 2 seconds
    public void checkTimeouts() {
        Instant now = Instant.now();

        pendingCommands.entrySet().removeIf(entry -> {
            PendingCommand pending = entry.getValue();
            long elapsed = now.toEpochMilli() - pending.getCreatedAt().toEpochMilli();

            if (elapsed > COMMAND_TIMEOUT_MS) {
                log.warn("Scene command timed out: {} for scene '{}' (received {}/{} acks)",
                    entry.getKey(), pending.getSceneName(),
                    pending.getAckCount(), pending.getLightsAffected());

                // Broadcast timeout status via WebSocket
                webSocketEventService.broadcastSceneTimeout(
                    pending.getSceneId(),
                    pending.getSceneName(),
                    entry.getKey(),
                    pending.getAckCount(),
                    pending.getLightsAffected()
                );

                return true; // Remove from map
            }
            return false;
        });
    }

    /**
     * Get pending command count (for monitoring/debugging).
     */
    public int getPendingCount() {
        return pendingCommands.size();
    }

    @Getter
    private static class PendingCommand {
        private final String correlationId;
        private final UUID sceneId;
        private final String sceneName;
        private final int lightsAffected;
        private final Instant createdAt;
        private int ackCount = 0;

        PendingCommand(String correlationId, UUID sceneId, String sceneName,
                            int lightsAffected, Instant createdAt) {
            this.correlationId = correlationId;
            this.sceneId = sceneId;
            this.sceneName = sceneName;
            this.lightsAffected = lightsAffected;
            this.createdAt = createdAt;
        }

        public synchronized void incrementAckCount() {
            this.ackCount++;
        }
    }
}
