package com.example.smart.lighting.scenes.with_natural.language.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Application context initializer that loads environment variables from .env files.
 *
 * <p>Searches for .env files in the following order:</p>
 * <ol>
 *   <li>Parent directory (project root when running from backend/)</li>
 *   <li>Current working directory</li>
 * </ol>
 *
 * <p>Loaded properties are added as the highest priority property source,
 * allowing them to override application.properties values.</p>
 *

 * @see ApplicationContextInitializer
 */
@Component
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Initializes the application context by loading .env file properties.
     *
     * @param applicationContext the Spring application context to configure
     */
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        // Try to load .env from parent directory (project root)
        File envFile = new File("../.env");
        if (!envFile.exists()) {
            // Try current directory
            envFile = new File(".env");
        }

        if (envFile.exists()) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(envFile));

                Map<String, Object> envMap = new HashMap<>();
                props.forEach((key, value) -> envMap.put(key.toString(), value));

                environment.getPropertySources().addFirst(new MapPropertySource("dotenv", envMap));
                System.out.println("Loaded .env file from: " + envFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to load .env file: " + e.getMessage());
            }
        } else {
            System.out.println("No .env file found");
        }
    }
}
