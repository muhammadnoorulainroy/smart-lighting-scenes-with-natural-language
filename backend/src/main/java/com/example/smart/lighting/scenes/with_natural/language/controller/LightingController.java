package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.service.MqttService;
import com.example.smart.lighting.scenes.with_natural.language.service.OpenAIService;
import com.example.smart.lighting.scenes.with_natural.language.service.AutomationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lighting")
@RequiredArgsConstructor
public class LightingController {

    private final MqttService mqttService;
    private final OpenAIService openAIService;
    private final AutomationService automationService;

    /**
     * Natural language command endpoint
     */
    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(@RequestBody CommandRequest request) {
        log.info("Received command: {}", request.command());

        try {
            // Parse command with OpenAI
            List<String> availableRooms = List.of("Living", "Bedroom", "Kitchen", "Bath", "Hallway");
            OpenAIService.ParsedCommand parsed = openAIService.parseCommand(request.command(), availableRooms);

            if ("error".equals(parsed.action())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", parsed.error()
                ));
            }

            // Execute command
            String controllerId = request.controllerId() != null ? request.controllerId() : "esp32-001";
            executeCommand(controllerId, parsed);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "parsed", Map.of(
                    "action", parsed.action(),
                    "room", parsed.room(),
                    "color", parsed.color(),
                    "brightness", parsed.brightness(),
                    "rgb", parsed.rgb()
                )
            ));
        } catch (Exception e) {
            log.error("Error processing command: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Direct LED control
     */
    @PostMapping("/led/{controllerId}/{ledIndex}")
    public ResponseEntity<?> controlLed(
        @PathVariable String controllerId,
        @PathVariable int ledIndex,
        @RequestBody LedControlRequest request
    ) {
        log.info("LED control: {} LED {} - RGB={}, brightness={}, on={}",
            controllerId, ledIndex, request.rgb(), request.brightness(), request.on());

        mqttService.sendLedCommand(
            controllerId,
            ledIndex,
            new MqttService.LedCommand(request.rgb(), request.brightness(), request.on())
        );

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Global control (all LEDs)
     */
    @PostMapping("/global/{controllerId}")
    public ResponseEntity<?> globalControl(
        @PathVariable String controllerId,
        @RequestBody GlobalControlRequest request
    ) {
        log.info("Global control: {} - action={}, brightness={}, mode={}",
            controllerId, request.action(), request.brightness(), request.mode());

        mqttService.sendGlobalCommand(
            controllerId,
            new MqttService.GlobalCommand(request.action(), request.brightness(), request.mode())
        );

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Activate scene
     */
    @PostMapping("/scene/{controllerId}/{sceneName}")
    public ResponseEntity<?> activateScene(
        @PathVariable String controllerId,
        @PathVariable String sceneName
    ) {
        log.info("Activating scene: {} on {}", sceneName, controllerId);
        mqttService.sendSceneCommand(controllerId, sceneName);
        return ResponseEntity.ok(Map.of("success", true, "scene", sceneName));
    }

    /**
     * Execute automation
     */
    @PostMapping("/automation/{automationId}/execute")
    public ResponseEntity<?> executeAutomation(@PathVariable String automationId) {
        log.info("Executing automation: {}", automationId);
        automationService.executeAutomation(automationId);
        return ResponseEntity.ok(Map.of("success", true, "automation", automationId));
    }

    /**
     * Execute parsed command
     */
    private void executeCommand(String controllerId, OpenAIService.ParsedCommand parsed) {
        switch (parsed.action()) {
            case "turn_on":
                mqttService.sendGlobalCommand(controllerId, new MqttService.GlobalCommand("on", null, null));
                break;

            case "turn_off":
                mqttService.sendGlobalCommand(controllerId, new MqttService.GlobalCommand("off", null, null));
                break;

            case "set_brightness":
                if (parsed.brightness() != null) {
                    mqttService.sendGlobalCommand(
                        controllerId,
                        new MqttService.GlobalCommand("brightness", parsed.brightness(), null)
                    );
                }
                break;

            case "set_color":
                if (parsed.rgb() != null) {
                    int ledIndex = getLedIndexForRoom(parsed.room());
                    int brightness = parsed.brightness() != null ? parsed.brightness() : 50;
                    mqttService.sendLedCommand(
                        controllerId,
                        ledIndex,
                        new MqttService.LedCommand(parsed.rgb(), brightness, true)
                    );
                }
                break;

            default:
                log.warn("Unknown action: {}", parsed.action());
        }
    }

    /**
     * Map room name to LED index
     */
    private int getLedIndexForRoom(String room) {
        if (room == null) return 0;
        return switch (room.toLowerCase()) {
            case "living" -> 0;
            case "bedroom" -> 1;
            case "kitchen" -> 2;
            case "bath", "bathroom" -> 3;
            case "hallway", "hall" -> 4;
            default -> 0;
        };
    }

    // Request DTOs
    public record CommandRequest(String command, String controllerId) {}
    public record LedControlRequest(int[] rgb, int brightness, boolean on) {}
    public record GlobalControlRequest(String action, Integer brightness, String mode) {}
}

