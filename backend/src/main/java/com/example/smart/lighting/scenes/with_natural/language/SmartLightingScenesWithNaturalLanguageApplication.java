package com.example.smart.lighting.scenes.with_natural.language;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the Smart Lighting system.
 *
 * <p>A Spring Boot application that provides:</p>
 * <ul>
 *   <li>REST API for lighting control and management</li>
 *   <li>OAuth2 and local authentication</li>
 *   <li>MQTT integration with ESP32 controllers</li>
 *   <li>WebSocket for real-time updates</li>
 *   <li>Natural language command processing via OpenAI</li>
 *   <li>Scheduled automation execution</li>
 * </ul>
 *
 * <h3>Key Technologies:</h3>
 * <ul>
 *   <li>Spring Boot 3.5.6 with Java 21</li>
 *   <li>PostgreSQL for persistence</li>
 *   <li>Redis for caching (optional)</li>
 *   <li>Eclipse Mosquitto for MQTT</li>
 * </ul>
 *

 */
@SpringBootApplication
@EnableScheduling
public final class SmartLightingScenesWithNaturalLanguageApplication {

    private SmartLightingScenesWithNaturalLanguageApplication() {
        // Utility class - prevent instantiation
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartLightingScenesWithNaturalLanguageApplication.class, args);
    }
}
