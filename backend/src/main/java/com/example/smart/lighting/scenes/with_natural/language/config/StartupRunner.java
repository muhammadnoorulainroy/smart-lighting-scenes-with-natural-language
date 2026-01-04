package com.example.smart.lighting.scenes.with_natural.language.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Startup runner that displays application configuration information.
 *
 * <p>Executed after the Spring context is fully initialized, this component
 * logs helpful URLs and configuration details for developers.</p>
 *
 * <h3>Displayed Information:</h3>
 * <ul>
 *   <li>API base URL and WebSocket endpoint</li>
 *   <li>OAuth test page location</li>
 *   <li>Infrastructure service ports (PostgreSQL, Redis, MQTT)</li>
 *   <li>Frontend development server URL</li>
 *   <li>Setup reminders for OAuth credentials</li>
 * </ul>
 *

 * @see CommandLineRunner
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupRunner implements CommandLineRunner {

    private final Environment env;

    /**
     * Runs on application startup to display configuration information.
     *
     * @param args command line arguments (not used)
     * @throws Exception if an error occurs during execution
     */
    @Override
    public void run(String... args) throws Exception {
        String port = env.getProperty("server.port", "8080");

        log.info("================================================");
        log.info("  Smart Lighting Backend Started Successfully!");
        log.info("================================================");
        log.info("  API URL:        http://localhost:{}/api", port);
        log.info("  OAuth Test:     http://localhost:{}/test-auth.html", port);
        log.info("  WebSocket:      ws://localhost:{}/ws", port);
        log.info("  Health Check:   http://localhost:{}/actuator/health", port);
        log.info("================================================");
        log.info("  Database:       PostgreSQL on port 5432");
        log.info("  Redis:          Redis on port 6379");
        log.info("  MQTT:           Mosquitto on port 1883");
        log.info("================================================");
        log.info("  Frontend:       http://localhost:5173");
        log.info("================================================");
        log.info("");
        log.info("  IMPORTANT: Before testing OAuth:");
        log.info("  1. Set up Google OAuth credentials");
        log.info("  2. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET");
        log.info("  3. Start Docker services: docker-compose -f infra/docker-compose.yml up -d");
        log.info("");
        log.info("================================================");
    }
}
