package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.SceneDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.SceneRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.MqttService;
import com.example.smart.lighting.scenes.with_natural.language.service.SceneCommandTracker;
import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing lighting scenes.
 *

 */
@RestController
@RequestMapping("/api/scenes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class ScenesController {

    private final SceneRepository sceneRepository;
    private final UserRepository userRepository;
    private final MqttService mqttService;
    private final SceneCommandTracker sceneCommandTracker;
    private final WebSocketEventService webSocketEventService;

    /**
     * Get all active scenes.
     * All authenticated users can view scenes.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SceneDto>> getAllScenes() {
        List<Scene> scenes = sceneRepository.findByIsActiveTrue();
        List<SceneDto> dtos = scenes.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a scene by ID.
     * All authenticated users can view scenes.
     */
    @GetMapping("/{sceneId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SceneDto> getSceneById(@PathVariable UUID sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found"));
        return ResponseEntity.ok(toDto(scene));
    }

    /**
     * Create a new scene.
     * OWNER and RESIDENT can create scenes.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<SceneDto> createScene(@RequestBody SceneDto sceneDto, Authentication auth) {
        log.info("Creating scene: {}", sceneDto.getName());

        User user = getCurrentUser(auth);

        Scene scene = Scene.builder()
            .name(sceneDto.getName())
            .description(sceneDto.getDescription())
            .icon(sceneDto.getIcon() != null ? sceneDto.getIcon() : "💡")
            .settingsJson(sceneDto.getSettings() != null ? sceneDto.getSettings() : new HashMap<>())
            .isPreset(false)
            .isActive(true)
            .createdBy(user)
            .build();

        scene = sceneRepository.save(scene);
        log.info("Scene created: {}", scene.getId());

        // Broadcast real-time event
        webSocketEventService.broadcastSceneCreated(scene.getId(), scene.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(scene));
    }

    /**
     * Update an existing scene.
     * OWNER and RESIDENT can update scenes.
     */
    @PutMapping("/{sceneId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<SceneDto> updateScene(@PathVariable UUID sceneId, @RequestBody SceneDto sceneDto) {
        Scene scene = sceneRepository.findById(sceneId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found"));

        if (Boolean.TRUE.equals(scene.getIsPreset())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Preset scenes cannot be modified");
        }

        if (sceneDto.getName() != null) {
            scene.setName(sceneDto.getName());
        }
        if (sceneDto.getDescription() != null) {
            scene.setDescription(sceneDto.getDescription());
        }
        if (sceneDto.getIcon() != null) {
            scene.setIcon(sceneDto.getIcon());
        }
        if (sceneDto.getSettings() != null) {
            scene.setSettingsJson(sceneDto.getSettings());
        }

        scene = sceneRepository.save(scene);
        
        // Broadcast real-time event
        webSocketEventService.broadcastSceneUpdated(scene.getId(), scene.getName());
        
        return ResponseEntity.ok(toDto(scene));
    }

    /**
     * Delete a scene (soft delete).
     * OWNER and RESIDENT can delete scenes.
     */
    @DeleteMapping("/{sceneId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<Void> deleteScene(@PathVariable UUID sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found"));

        if (Boolean.TRUE.equals(scene.getIsPreset())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Preset scenes cannot be deleted");
        }

        scene.setIsActive(false);
        sceneRepository.save(scene);

        // Broadcast real-time event
        webSocketEventService.broadcastSceneDeleted(sceneId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Apply a scene to the lights.
     * All authenticated users (including GUEST) can apply scenes.
     */
    @PostMapping("/{sceneId}/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> applyScene(@PathVariable UUID sceneId) {
        Scene scene = sceneRepository.findById(sceneId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found"));

        log.info("Applying scene: {}", scene.getName());

        Map<String, Object> settings = scene.getSettingsJson();

        // Build command
        Map<String, Object> command = new HashMap<>();
        command.put("on", true);
        // Scenes don't set mode - sensors continue adjusting based on scene settings

        if (settings.containsKey("brightness")) {
            command.put("brightness", settings.get("brightness"));
        }
        if (settings.containsKey("rgb")) {
            command.put("rgb", settings.get("rgb"));
        }
        if (settings.containsKey("color_temp")) {
            command.put("color_temp", settings.get("color_temp"));
        }

        // Determine target LEDs
        Object target = settings.getOrDefault("target", "all");
        List<Integer> ledIndices = getLedIndicesForTarget(target);

        // Register command for tracking acks
        String correlationId = sceneCommandTracker.registerCommand(
            scene.getId(),
            scene.getName(),
            ledIndices.size()
        );

        // Add correlation ID to command for ESP32 to echo back
        command.put("correlationId", correlationId);

        // Send commands to each LED
        for (int ledIndex : ledIndices) {
            mqttService.publishLedCommand(ledIndex, command);
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Applied scene '" + scene.getName() + "'",
            "lightsAffected", ledIndices.size(),
            "correlationId", correlationId,
            "status", "pending"
        ));
    }

    /**
     * Get LED indices for a target.
     */
    private List<Integer> getLedIndicesForTarget(Object target) {
        if (target == null || "all".equalsIgnoreCase(target.toString())) {
            return List.of(0, 1, 2, 3, 4);
        }

        String room = target.toString().toLowerCase().replace(" ", "_").replace("-", "_");

        return switch (room) {
            case "living_room", "living" -> List.of(0);
            case "bedroom" -> List.of(1);
            case "kitchen" -> List.of(2);
            case "bathroom", "bath" -> List.of(3);
            case "hallway" -> List.of(4);
            default -> List.of(0, 1, 2, 3, 4);
        };
    }

    /**
     * Get current user from authentication.
     */
    private User getCurrentUser(Authentication auth) {
        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    /**
     * Convert entity to DTO.
     */
    private SceneDto toDto(Scene scene) {
        SceneDto.SceneDtoBuilder builder = SceneDto.builder()
            .id(scene.getId())
            .name(scene.getName())
            .description(scene.getDescription())
            .icon(scene.getIcon())
            .settings(scene.getSettingsJson())
            .isPreset(scene.getIsPreset())
            .createdAt(scene.getCreatedAt())
            .updatedAt(scene.getUpdatedAt());

        if (scene.getCreatedBy() != null) {
            builder.createdBy(scene.getCreatedBy().getId())
                .createdByName(scene.getCreatedBy().getName());
        }

        return builder.build();
    }
}
