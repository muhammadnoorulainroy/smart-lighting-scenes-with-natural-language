package com.example.smart.lighting.scenes.with_natural.language.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time frontend communication.
 *
 * <p>Enables STOMP over WebSocket for pushing device state updates,
 * sensor readings, and scene status to connected clients.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code /ws} - SockJS-enabled endpoint for browser clients</li>
 *   <li>{@code /ws-native} - Raw WebSocket endpoint for native clients</li>
 * </ul>
 *
 * <h3>Topic Prefixes:</h3>
 * <ul>
 *   <li>{@code /topic/*} - Broadcast messages to all subscribers</li>
 *   <li>{@code /queue/*} - Point-to-point messages</li>
 *   <li>{@code /app/*} - Application destination prefix</li>
 * </ul>
 *

 * @see WebSocketMessageBrokerConfigurer
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Configures the message broker with topic and queue destinations.
     *
     * @param config the message broker registry to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker
        config.enableSimpleBroker("/topic", "/queue");
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers STOMP endpoints for WebSocket connections.
     *
     * @param registry the endpoint registry to configure
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws endpoint for WebSocket connections
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS(); // Enable SockJS fallback

        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint("/ws-native")
                .setAllowedOrigins(allowedOrigins);

        log.info("WebSocket endpoints registered at /ws and /ws-native");
    }
}
