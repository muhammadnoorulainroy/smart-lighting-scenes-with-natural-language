package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for system configuration management.
 *
 * <p>Manages runtime configuration for ESP32 devices including:</p>
 * <ul>
 *   <li>Sensor thresholds and calibration values</li>
 *   <li>LED brightness limits and color temperature ranges</li>
 *   <li>MQTT topic prefixes and timing settings</li>
 *   <li>Power saving and display timeout settings</li>
 * </ul>
 *
 * <p>Configuration changes are automatically synced to connected devices via MQTT.</p>
 *

 * @see ConfigService
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class ConfigController {

    private final ConfigService configService;

    /**
     * Get all configuration settings.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<Map<String, Object>> getAllConfig() {
        return ResponseEntity.ok(configService.getAllConfig());
    }

    /**
     * Get configuration for a specific category.
     */
    @GetMapping("/{category}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String category) {
        return configService.getConfig(category)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update configuration for a specific category.
     */
    @PutMapping("/{category}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String category,
            @RequestBody Map<String, Object> updates,
            Authentication auth) {

        String updatedBy = getUsername(auth);
        log.info("Config update for '{}' by {}", category, updatedBy);

        Map<String, Object> result = configService.updateConfig(category, updates, updatedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * Update all configuration categories at once.
     */
    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> updateAllConfig(
            @RequestBody Map<String, Map<String, Object>> allUpdates,
            Authentication auth) {

        String updatedBy = getUsername(auth);
        log.info("Full config update by {}", updatedBy);

        Map<String, Object> result = configService.updateAllConfig(allUpdates, updatedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * Reset a category to defaults.
     */
    @PostMapping("/{category}/reset")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> resetCategory(@PathVariable String category) {
        log.info("Resetting config '{}' to defaults", category);
        Map<String, Object> result = configService.resetToDefaults(category);
        return ResponseEntity.ok(result);
    }

    /**
     * Reset all categories to defaults.
     */
    @PostMapping("/reset")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> resetAll() {
        log.info("Resetting all config to defaults");
        Map<String, Object> result = configService.resetAllToDefaults();
        return ResponseEntity.ok(result);
    }

    /**
     * Manually push current config to ESP32 devices.
     */
    @PostMapping("/sync")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, String>> syncToDevices() {
        log.info("Manual config sync requested");
        configService.publishFullConfigUpdate();
        return ResponseEntity.ok(Map.of("status", "Config published to devices"));
    }

    private String getUsername(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        }
        return "unknown";
    }
}
