package com.example.smart.lighting.scenes.with_natural.language;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the Smart Lighting system.
 */
@SpringBootApplication
public final class SmartLightingScenesWithNaturalLanguageApplication {

    private SmartLightingScenesWithNaturalLanguageApplication() {
        // Utility class - prevent instantiation
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartLightingScenesWithNaturalLanguageApplication.class, args);
    }
}
