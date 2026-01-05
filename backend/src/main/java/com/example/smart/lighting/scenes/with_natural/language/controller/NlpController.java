package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.NlpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for natural language command processing.
 * Handles voice and text commands for lighting control.
 *

 */
@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class NlpController {

    private final NlpService nlpService;
    private final UserRepository userRepository;

    /**
     * Parse a natural language command without executing it.
     * Returns a preview of what the command will do.
     * All authenticated users can parse commands.
     *
     * @param request The request containing the text command
     * @return Parsed command with preview
     */
    @PostMapping("/parse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NlpCommandDto> parseCommand(@RequestBody Map<String, String> request) {
        String text = request.get("text");

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(
                NlpCommandDto.builder()
                    .valid(false)
                    .error("No command provided")
                    .build()
            );
        }

        log.info("Parsing command: {}", text);
        NlpCommandDto result = nlpService.parseCommand(text);

        return ResponseEntity.ok(result);
    }

    /**
     * Execute a natural language command.
     * First parses the command, then executes if valid.
     * All authenticated users can execute immediate commands (light control, apply scenes).
     * Schedule creation requires OWNER or RESIDENT role (enforced in service layer).
     *
     * @param request The request containing the text command
     * @param auth Authentication context
     * @return Execution result
     */
    @PostMapping("/execute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NlpCommandDto> executeCommand(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        String text = request.get("text");

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(
                NlpCommandDto.builder()
                    .valid(false)
                    .error("No command provided")
                    .build()
            );
        }

        log.info("Executing command: {}", text);

        // First parse
        NlpCommandDto parsed = nlpService.parseCommand(text);

        if (!Boolean.TRUE.equals(parsed.getValid())) {
            return ResponseEntity.ok(parsed);
        }

        // Then execute
        User user = getCurrentUser(auth);
        NlpCommandDto result = nlpService.executeCommand(parsed, user);

        return ResponseEntity.ok(result);
    }

    /**
     * Parse and execute a command in one step (convenience endpoint).
     * Skips the preview/confirm flow.
     * All authenticated users can execute immediate commands.
     *
     * @param request The request containing the text command
     * @param auth Authentication context
     * @return Execution result
     */
    @PostMapping("/command")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NlpCommandDto> command(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        return executeCommand(request, auth);
    }

    /**
     * Confirm and execute a previously parsed command.
     * All authenticated users can confirm immediate commands.
     * Schedule creation requires OWNER or RESIDENT role (enforced in service layer).
     *
     * @param commandDto The parsed command to execute
     * @param auth Authentication context
     * @return Execution result
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NlpCommandDto> confirmCommand(
            @RequestBody NlpCommandDto commandDto,
            Authentication auth) {

        if (!Boolean.TRUE.equals(commandDto.getValid())) {
            return ResponseEntity.badRequest().body(commandDto);
        }

        User user = getCurrentUser(auth);
        NlpCommandDto result = nlpService.executeCommand(commandDto, user);

        return ResponseEntity.ok(result);
    }

    /**
     * Apply a conflict resolution for a schedule.
     * Only OWNER and RESIDENT can resolve schedule conflicts.
     *
     * @param request The resolution request containing scheduleId, resolutionId, and params
     * @return Result of applying the resolution
     */
    @PostMapping("/resolve-conflict")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<Map<String, Object>> resolveConflict(@RequestBody Map<String, Object> request) {
        String scheduleIdStr = (String) request.get("scheduleId");
        String resolutionId = (String) request.get("resolutionId");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        if (scheduleIdStr == null || resolutionId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Missing scheduleId or resolutionId"
            ));
        }

        try {
            java.util.UUID scheduleId = java.util.UUID.fromString(scheduleIdStr);
            String result = nlpService.applyConflictResolution(scheduleId, resolutionId, params);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result
            ));
        } catch (Exception e) {
            log.error("Error applying resolution: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get current user from authentication.
     */
    private User getCurrentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }
}
