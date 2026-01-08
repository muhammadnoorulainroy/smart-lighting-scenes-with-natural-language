package com.example.smart.lighting.scenes.with_natural.language.service.nlp;

import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ParsedCommand;
import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import com.example.smart.lighting.scenes.with_natural.language.repository.SceneRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.MqttService;
import com.example.smart.lighting.scenes.with_natural.language.service.SceneCommandTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executes parsed NLP commands via MQTT.
 *

 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NlpCommandExecutor {

    private final MqttService mqttService;
    private final SceneRepository sceneRepository;
    private final SceneCommandTracker sceneCommandTracker;
    private final NlpColorUtils colorUtils;

    /**
     * Execute an immediate (non-scheduled) command.
     *
     * @param parsed the parsed command
     * @return result message
     */
    public String executeImmediateCommand(ParsedCommand parsed) {
        String intent = parsed.getIntent();
        Object target = parsed.getTarget();
        Map<String, Object> params = parsed.getParams() != null
            ? parsed.getParams() : new HashMap<>();

        Map<String, Object> command = new HashMap<>();

        switch (intent) {
            case "light.on" -> command.put("on", true);
            case "light.off" -> command.put("on", false);
            case "light.brightness" -> {
                command.put("on", true);
                command.put("brightness", params.get("brightness"));
            }
            case "light.color" -> {
                command.put("on", true);
                Object rgb = params.get("rgb");
                if (rgb == null && params.get("color") != null) {
                    rgb = colorUtils.colorNameToRgb(params.get("color").toString());
                }
                if (rgb != null) {
                    command.put("rgb", rgb);
                }
            }
            case "light.color_temp" -> {
                command.put("on", true);
                command.put("color_temp", params.get("color_temp"));
            }
            case "scene.apply" -> {
                return applyScene(parsed.getScene(), target != null ? target.toString() : null);
            }
            default -> throw new IllegalArgumentException("Unknown intent: " + intent);
        }

        List<Integer> ledIndices = getLedIndicesForTarget(target);

        String commandName = "NLP: " + intent;
        String correlationId = sceneCommandTracker.registerCommand(null, commandName, ledIndices.size());

        for (int ledIndex : ledIndices) {
            command.put("correlationId", correlationId);
            mqttService.publishLedCommand(ledIndex, new HashMap<>(command));
        }

        return "Command sent to " + ledIndices.size() + " light(s)";
    }

    /**
     * Apply a scene by name to a specific target.
     *
     * @param sceneName the scene name
     * @param userTarget the target room (or null for all)
     * @return result message
     */
    public String applyScene(String sceneName, String userTarget) {
        Optional<Scene> sceneOpt = sceneRepository.findByNameIgnoreCaseAndIsActiveTrue(sceneName);
        if (sceneOpt.isEmpty()) {
            throw new IllegalArgumentException("Scene not found: " + sceneName);
        }

        Scene scene = sceneOpt.get();
        Map<String, Object> settings = scene.getSettingsJson();

        Map<String, Object> command = new HashMap<>();
        if (settings.containsKey("brightness")) {
            command.put("brightness", settings.get("brightness"));
        }
        if (settings.containsKey("rgb")) {
            command.put("rgb", settings.get("rgb"));
        }
        if (settings.containsKey("color_temp")) {
            command.put("color_temp", settings.get("color_temp"));
        }
        command.put("on", true);

        Object effectiveTarget;
        if (userTarget != null && !userTarget.isBlank()) {
            effectiveTarget = userTarget;
        } else {
            effectiveTarget = settings.getOrDefault("target", "all");
        }

        List<Integer> ledIndices = getLedIndicesForTarget(effectiveTarget);

        String correlationId = sceneCommandTracker.registerCommand(
            scene.getId(), scene.getName(), ledIndices.size());

        for (int ledIndex : ledIndices) {
            command.put("correlationId", correlationId);
            mqttService.publishLedCommand(ledIndex, new HashMap<>(command));
        }

        String targetDesc = "all".equalsIgnoreCase(effectiveTarget.toString())
            ? "all lights"
            : effectiveTarget + " (" + ledIndices.size() + " light(s))";
        return "Applied scene '" + scene.getName() + "' to " + targetDesc;
    }

    /**
     * Get LED indices for a target (room name or "all").
     *
     * @param target the target room or "all"
     * @return list of LED indices
     */
    public List<Integer> getLedIndicesForTarget(Object target) {
        if (target == null || "all".equalsIgnoreCase(target.toString())) {
            return List.of(0, 1, 2, 3, 4);
        }

        String room = target.toString().toLowerCase()
            .replace(" ", "_").replace("-", "_");

        return switch (room) {
            case "kitchen" -> List.of(0);
            case "bedroom" -> List.of(1);
            case "bathroom", "bath" -> List.of(2);
            case "hallway" -> List.of(3);
            case "living_room", "living" -> List.of(4);
            default -> List.of(0, 1, 2, 3, 4);
        };
    }
}

