package com.example.smart.lighting.scenes.with_natural.language.controller;
import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.MqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.UUID;
/**
* REST controller for controlling smart lighting devices.
* Provides endpoints to send commands to LED devices via MQTT.
*
* @author Smart Lighting Team
* @version 1.0
*/
@RestController
@RequestMapping("/api/lighting")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
public class LightingController {
    private final DeviceRepository deviceRepository;
    private final MqttService mqttService;
    /**
     * Send a command to control an LED device.
     *
     * @param deviceId The device UUID
     * @param command The LED command containing on, rgb, brightness, color_temp, mode
     * @return Success response
     */
    @PostMapping("/devices/{deviceId}/command")
    public ResponseEntity<Map<String, Object>> sendLedCommand(
            @PathVariable UUID deviceId,
            @RequestBody Map<String, Object> command) {
        log.info("Received LED command for device {}: {}", deviceId, command);
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        // Get LED index from device meta_json
        Integer ledIndex = null;
        if (device.getMetaJson() != null && device.getMetaJson().containsKey("led_index")) {
            ledIndex = ((Number) device.getMetaJson().get("led_index")).intValue();
        }
        if (ledIndex == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device does not have LED index configured");
        }
        // Publish command to MQTT
        mqttService.publishLedCommand(ledIndex, command);
        log.info("LED command published for LED index {}", ledIndex);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Command sent to LED " + ledIndex,
            "ledIndex", ledIndex
        ));
    }
    /**
     * Set power state for an LED device.
     *
     * @param deviceId The device UUID
     * @param body Request body containing 'on' boolean
     * @return Success response
     */
    @PostMapping("/devices/{deviceId}/power")
    public ResponseEntity<Map<String, Object>> setPower(
            @PathVariable UUID deviceId,
            @RequestBody Map<String, Object> body) {
        Boolean on = (Boolean) body.get("on");
        if (on == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'on' field");
        }
        return sendLedCommand(deviceId, Map.of("on", on));
    }
    /**
     * Set a scene/preset for an LED device.
     *
     * @param deviceId The device UUID
     * @param body Request body containing 'scene' name
     * @return Success response
     */
    @PostMapping("/devices/{deviceId}/scene")
    public ResponseEntity<Map<String, Object>> setScene(
            @PathVariable UUID deviceId,
            @RequestBody Map<String, Object> body) {
        String scene = (String) body.get("scene");
        if (scene == null || scene.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'scene' field");
        }
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        Integer ledIndex = null;
        if (device.getMetaJson() != null && device.getMetaJson().containsKey("led_index")) {
            ledIndex = ((Number) device.getMetaJson().get("led_index")).intValue();
        }
        if (ledIndex == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device does not have LED index configured");
        }
        // Publish scene command
        mqttService.publishSceneCommand(ledIndex, scene);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Scene '" + scene + "' applied to LED " + ledIndex
        ));
    }
    /**
     * Set global mode (auto/manual) for a controller.
     *
     * @param controllerId The controller ID
     * @param body Request body containing 'mode' (auto/manual)
     * @return Success response
     */
    @PostMapping("/controllers/{controllerId}/mode")
    public ResponseEntity<Map<String, Object>> setMode(
            @PathVariable String controllerId,
            @RequestBody Map<String, Object> body) {
        String mode = (String) body.get("mode");
        if (mode == null || (!mode.equals("auto") && !mode.equals("manual"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode must be 'auto' or 'manual'");
        }
        // Publish mode command
        mqttService.publishModeCommand(controllerId, mode);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Mode set to '" + mode + "' for controller " + controllerId
        ));
    }
    /**
     * Set global mode (auto/manual) for all LEDs.
     * Simplified endpoint that doesn't require a controller ID.
     *
     * @param body Request body containing 'mode' (auto/manual)
     * @return Success response
     */
    @PostMapping("/mode")
    public ResponseEntity<Map<String, Object>> setGlobalMode(@RequestBody Map<String, Object> body) {
        String mode = (String) body.get("mode");
        if (mode == null || (!mode.equals("auto") && !mode.equals("manual"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode must be 'auto' or 'manual'");
        }
        // Publish global mode command
        mqttService.publishModeCommand("global", mode);
        log.info("Global mode set to: {}", mode);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "All lights switched to '" + mode + "' mode"
        ));
    }
}