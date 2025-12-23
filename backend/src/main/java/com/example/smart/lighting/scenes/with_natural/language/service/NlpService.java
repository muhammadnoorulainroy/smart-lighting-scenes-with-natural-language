package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.dto.ConflictAnalysisDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ParsedCommand;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ScheduleConfig;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictAnalysisResult;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictResolution;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ScheduleConflict;
import com.example.smart.lighting.scenes.with_natural.language.entity.NlpCommand;
import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.NlpCommandRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.SceneRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for parsing natural language commands using OpenAI.
 * Converts user commands into structured lighting actions.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NlpService {

    private final ObjectMapper objectMapper;
    private final NlpCommandRepository nlpCommandRepository;
    private final SceneRepository sceneRepository;
    private final ScheduleRepository scheduleRepository;
    private final MqttService mqttService;
    private final ScheduleConflictService conflictService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openaiModel;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private static final List<String> AVAILABLE_ROOMS = List.of(
        "bedroom", "living_room", "living-room", "kitchen", "bathroom", "hallway", "all"
    );

    /**
     * Parse a natural language command without executing it.
     * Returns a preview of what the command will do.
     */
    public NlpCommandDto parseCommand(String text) {
        log.info("Parsing NLP command: {}", text);
        
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return NlpCommandDto.builder()
                .text(text)
                .valid(false)
                .error("OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.")
                .timestamp(LocalDateTime.now())
                .build();
        }

        try {
            // Get available scenes for context
            List<Scene> scenes = sceneRepository.findByIsActiveTrue();
            List<String> sceneNames = scenes.stream().map(Scene::getName).toList();

            // Build the prompt
            String prompt = buildPrompt(text, sceneNames);
            
            // Call OpenAI API
            String response = callOpenAI(prompt);
            log.debug("OpenAI response: {}", response);
            
            // Parse the response
            ParsedCommand parsed = parseOpenAIResponse(response);
            
            if (parsed == null) {
                return NlpCommandDto.builder()
                    .text(text)
                    .valid(false)
                    .error("Could not understand the command. Please try rephrasing.")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Validate the parsed command
            String validationError = validateParsedCommand(parsed);
            if (validationError != null) {
                return NlpCommandDto.builder()
                    .text(text)
                    .parsed(parsed)
                    .valid(false)
                    .error(validationError)
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Generate preview
            String preview = generatePreview(parsed);
            boolean isScheduled = parsed.getSchedule() != null;
            
            // Check for conflicts if this is a scheduled command
            ConflictAnalysisDto conflictAnalysis = null;
            if (isScheduled) {
                conflictAnalysis = checkForScheduleConflicts(parsed);
                if (conflictAnalysis != null && conflictAnalysis.isHasConflicts()) {
                    preview += "\n\n" + conflictAnalysis.getSummary();
                }
            }
            
            return NlpCommandDto.builder()
                .text(text)
                .parsed(parsed)
                .preview(preview)
                .valid(true)
                .isScheduled(isScheduled)
                .conflictAnalysis(conflictAnalysis)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing NLP command: {}", e.getMessage(), e);
            return NlpCommandDto.builder()
                .text(text)
                .valid(false)
                .error("Error processing command: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Execute a parsed command.
     */
    public NlpCommandDto executeCommand(NlpCommandDto commandDto, User user) {
        if (!Boolean.TRUE.equals(commandDto.getValid())) {
            return commandDto;
        }

        ParsedCommand parsed = commandDto.getParsed();
        
        try {
            String result;
            
            if (Boolean.TRUE.equals(commandDto.getIsScheduled())) {
                // Create a schedule
                Schedule schedule = createScheduleFromParsed(parsed, user);
                result = "Created schedule: " + schedule.getName();
                commandDto.setResult(result);
            } else {
                // Execute immediately
                result = executeImmediateCommand(parsed);
                commandDto.setResult(result);
            }
            
            commandDto.setExecuted(true);
            
            // Save to history
            saveCommandHistory(commandDto, user);
            
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage(), e);
            commandDto.setExecuted(false);
            commandDto.setResult("Error: " + e.getMessage());
        }
        
        return commandDto;
    }

    /**
     * Build the prompt for OpenAI.
     */
    private String buildPrompt(String userInput, List<String> sceneNames) {
        return """
            You are a smart home lighting assistant. Parse the user's command into a structured JSON.
            
            Available rooms: bedroom, living_room, kitchen, bathroom, hallway, all
            Available scenes: %s
            Available actions:
            - light.on: Turn lights on
            - light.off: Turn lights off
            - light.brightness: Set brightness (0-100)
            - light.color: Set RGB color
            - light.color_temp: Set color temperature (2700-6500 Kelvin)
            - scene.apply: Apply a predefined scene
            - scene.create: Create a new scene
            
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "intent": "light.brightness",
              "target": "bedroom",
              "params": {"brightness": 30},
              "scene": null,
              "schedule": null,
              "confidence": 0.95
            }
            
            For scheduled commands, include:
            {
              "intent": "light.off",
              "target": "living_room",
              "params": {},
              "scene": null,
              "schedule": {
                "time": "07:00",
                "trigger": null,
                "offsetMinutes": null,
                "recurrence": "daily"
              },
              "confidence": 0.9
            }
            
            For sun-based triggers (sunset/sunrise):
            {
              "schedule": {
                "time": null,
                "trigger": "sunset",
                "offsetMinutes": -30,
                "recurrence": "daily"
              }
            }
            
            Rules:
            - For "all lights" or no room specified, use target: "all"
            - For colors, convert to RGB array [r, g, b]
            - For "warm" colors, use color_temp: 2700-3000
            - For "cool" colors, use color_temp: 5000-6500
            - For "every day" or "daily", use recurrence: "daily"
            - For "weekdays", use recurrence: "weekdays"
            - Set confidence based on how certain you are (0.0 to 1.0)
            
            User command: "%s"
            """.formatted(String.join(", ", sceneNames), userInput);
    }

    /**
     * Call OpenAI API.
     */
    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = Map.of(
            "model", openaiModel,
            "messages", List.of(
                Map.of("role", "system", "content", "You are a JSON-only response bot. Never include markdown or explanations."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "max_tokens", 500
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            request,
            Map.class
        );

        if (response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        
        throw new RuntimeException("No response from OpenAI");
    }

    /**
     * Parse the JSON response from OpenAI.
     */
    private ParsedCommand parseOpenAIResponse(String response) {
        try {
            // Clean up response (remove markdown if present)
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }
            
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            
            ParsedCommand.ParsedCommandBuilder builder = ParsedCommand.builder()
                .intent((String) map.get("intent"))
                .target(map.get("target"))
                .scene((String) map.get("scene"))
                .confidence(map.get("confidence") != null ? ((Number) map.get("confidence")).doubleValue() : 0.8);
            
            if (map.get("params") != null) {
                builder.params((Map<String, Object>) map.get("params"));
            }
            
            if (map.get("schedule") != null) {
                Map<String, Object> scheduleMap = (Map<String, Object>) map.get("schedule");
                ScheduleConfig schedule = ScheduleConfig.builder()
                    .time((String) scheduleMap.get("time"))
                    .trigger((String) scheduleMap.get("trigger"))
                    .offsetMinutes(scheduleMap.get("offsetMinutes") != null ? 
                        ((Number) scheduleMap.get("offsetMinutes")).intValue() : null)
                    .recurrence(scheduleMap.get("recurrence"))
                    .build();
                builder.schedule(schedule);
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate the parsed command.
     */
    private String validateParsedCommand(ParsedCommand parsed) {
        if (parsed.getIntent() == null || parsed.getIntent().isBlank()) {
            return "Could not determine what action to take. Please specify what you want to do with the lights.";
        }
        
        // Validate target for light commands
        if (parsed.getIntent().startsWith("light.")) {
            Object target = parsed.getTarget();
            if (target == null) {
                return "Please specify which room or 'all lights'.";
            }
            
            String targetStr = target.toString().toLowerCase().replace(" ", "_").replace("-", "_");
            if (!targetStr.equals("all") && !AVAILABLE_ROOMS.contains(targetStr)) {
                return "Unknown room: '" + target + "'. Available rooms: bedroom, living room, kitchen, bathroom, hallway, or 'all'.";
            }
        }
        
        // Validate brightness
        if ("light.brightness".equals(parsed.getIntent())) {
            Map<String, Object> params = parsed.getParams();
            if (params == null || !params.containsKey("brightness")) {
                return "Please specify a brightness level (0-100%).";
            }
            int brightness = ((Number) params.get("brightness")).intValue();
            if (brightness < 0 || brightness > 100) {
                return "Brightness must be between 0 and 100.";
            }
        }
        
        // Validate color - ensure RGB is provided
        if ("light.color".equals(parsed.getIntent())) {
            Map<String, Object> params = parsed.getParams();
            if (params == null) {
                return "Please specify a color.";
            }
            Object rgb = params.get("rgb");
            Object color = params.get("color");
            
            // If RGB is already a valid list, we're good
            if (rgb instanceof List<?> rgbList && rgbList.size() >= 3) {
                // RGB is valid, nothing to do
                log.debug("RGB already provided as list: {}", rgb);
            } 
            // If RGB is a string representation of an array like "[255, 0, 0]", parse it
            else if (rgb instanceof String rgbStr && rgbStr.startsWith("[")) {
                try {
                    List<Integer> rgbValues = parseRgbString(rgbStr);
                    if (rgbValues != null) {
                        params.put("rgb", rgbValues);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse RGB string: {}", rgbStr);
                }
            }
            // If color is provided as a name, convert it
            else if (color != null) {
                String colorStr = color.toString();
                // Check if color is actually an RGB array string
                if (colorStr.startsWith("[")) {
                    try {
                        List<Integer> rgbValues = parseRgbString(colorStr);
                        if (rgbValues != null) {
                            params.put("rgb", rgbValues);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse color as RGB: {}", colorStr);
                    }
                } else {
                    // Try to convert color name to RGB
                    List<Integer> rgbValues = colorNameToRgb(colorStr);
                    if (rgbValues != null) {
                        params.put("rgb", rgbValues);
                    } else {
                        return "Unknown color: '" + color + "'. Try 'red', 'blue', 'green', 'yellow', 'purple', 'orange', 'white', or 'pink'.";
                    }
                }
            }
            // No valid color found
            else if (rgb == null) {
                return "Please specify a color (e.g., 'red', 'blue', or RGB values).";
            }
        }
        
        // Validate scene
        if ("scene.apply".equals(parsed.getIntent())) {
            if (parsed.getScene() == null || parsed.getScene().isBlank()) {
                return "Please specify which scene to apply.";
            }
        }
        
        // Validate confidence
        if (parsed.getConfidence() != null && parsed.getConfidence() < 0.5) {
            return "I'm not confident I understood correctly. Please try rephrasing your command.";
        }
        
        return null; // Valid
    }

    /**
     * Generate a human-readable preview of the command.
     */
    private String generatePreview(ParsedCommand parsed) {
        StringBuilder preview = new StringBuilder();
        
        // Action description
        String intent = parsed.getIntent();
        Object target = parsed.getTarget();
        String targetStr = target != null ? target.toString() : "all";
        targetStr = targetStr.replace("_", " ");
        
        switch (intent) {
            case "light.on" -> preview.append("Turn ON ").append(targetStr).append(" lights");
            case "light.off" -> preview.append("Turn OFF ").append(targetStr).append(" lights");
            case "light.brightness" -> {
                int brightness = ((Number) parsed.getParams().get("brightness")).intValue();
                preview.append("Set ").append(targetStr).append(" lights to ").append(brightness).append("% brightness");
            }
            case "light.color" -> {
                Object rgbObj = parsed.getParams() != null ? parsed.getParams().get("rgb") : null;
                if (rgbObj instanceof List<?> rgb && rgb.size() >= 3) {
                    preview.append("Set ").append(targetStr).append(" lights to color RGB(")
                        .append(rgb.get(0)).append(", ").append(rgb.get(1)).append(", ").append(rgb.get(2)).append(")");
                } else {
                    Object color = parsed.getParams() != null ? parsed.getParams().get("color") : "custom";
                    preview.append("Set ").append(targetStr).append(" lights to ").append(color).append(" color");
                }
            }
            case "light.color_temp" -> {
                int colorTemp = ((Number) parsed.getParams().get("color_temp")).intValue();
                String warmCool = colorTemp < 4000 ? "warm" : colorTemp > 5000 ? "cool" : "neutral";
                preview.append("Set ").append(targetStr).append(" lights to ").append(warmCool)
                    .append(" white (").append(colorTemp).append("K)");
            }
            case "scene.apply" -> {
                preview.append("Apply '").append(parsed.getScene()).append("' scene to ").append(targetStr);
                if (targetStr.equalsIgnoreCase("all")) {
                    preview.append(" lights");
                }
            }
            case "scene.create" -> preview.append("Create new scene '").append(parsed.getScene()).append("'");
            default -> preview.append("Execute: ").append(intent);
        }
        
        // Schedule description
        ScheduleConfig schedule = parsed.getSchedule();
        if (schedule != null) {
            preview.append(" ");
            if (schedule.getTime() != null) {
                preview.append("at ").append(schedule.getTime());
            } else if (schedule.getTrigger() != null) {
                preview.append("at ").append(schedule.getTrigger());
                if (schedule.getOffsetMinutes() != null && schedule.getOffsetMinutes() != 0) {
                    int offset = schedule.getOffsetMinutes();
                    preview.append(" ").append(offset > 0 ? "+" : "").append(offset).append(" minutes");
                }
            }
            
            if (schedule.getRecurrence() != null) {
                Object recurrence = schedule.getRecurrence();
                if ("daily".equals(recurrence)) {
                    preview.append(", every day");
                } else if ("weekdays".equals(recurrence)) {
                    preview.append(", on weekdays");
                } else if ("weekends".equals(recurrence)) {
                    preview.append(", on weekends");
                } else if (recurrence instanceof List) {
                    preview.append(", on ").append(recurrence);
                }
            }
        }
        
        return preview.toString();
    }

    /**
     * Execute an immediate (non-scheduled) command.
     */
    private String executeImmediateCommand(ParsedCommand parsed) {
        String intent = parsed.getIntent();
        Object target = parsed.getTarget();
        Map<String, Object> params = parsed.getParams() != null ? parsed.getParams() : new HashMap<>();
        
        // Build MQTT command
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
                    rgb = colorNameToRgb(params.get("color").toString());
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
        
        // Determine LED indices
        List<Integer> ledIndices = getLedIndicesForTarget(target);
        
        // Send commands
        for (int ledIndex : ledIndices) {
            mqttService.publishLedCommand(ledIndex, command);
        }
        
        return "Command sent to " + ledIndices.size() + " light(s)";
    }

    /**
     * Apply a scene by name to a specific target (room or "all").
     * @param sceneName the name of the scene to apply
     * @param userTarget the user-specified target room (null means use scene's default or "all")
     */
    private String applyScene(String sceneName, String userTarget) {
        Optional<Scene> sceneOpt = sceneRepository.findByNameIgnoreCaseAndIsActiveTrue(sceneName);
        if (sceneOpt.isEmpty()) {
            throw new IllegalArgumentException("Scene not found: " + sceneName);
        }
        
        Scene scene = sceneOpt.get();
        Map<String, Object> settings = scene.getSettingsJson();
        
        // Build command from scene settings
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
        // Scenes don't set mode - sensors continue working with scene as base
        
        // Use user-specified target if provided, otherwise fall back to scene's default or "all"
        Object effectiveTarget;
        if (userTarget != null && !userTarget.isBlank()) {
            effectiveTarget = userTarget;
        } else {
            effectiveTarget = settings.getOrDefault("target", "all");
        }
        
        List<Integer> ledIndices = getLedIndicesForTarget(effectiveTarget);
        
        // Send commands
        for (int ledIndex : ledIndices) {
            mqttService.publishLedCommand(ledIndex, command);
        }
        
        String targetDesc = "all".equalsIgnoreCase(effectiveTarget.toString()) 
            ? "all lights" 
            : effectiveTarget + " (" + ledIndices.size() + " light(s))";
        return "Applied scene '" + scene.getName() + "' to " + targetDesc;
    }

    /**
     * Get LED indices for a target (room name or "all").
     */
    private List<Integer> getLedIndicesForTarget(Object target) {
        if (target == null || "all".equalsIgnoreCase(target.toString())) {
            return List.of(0, 1, 2, 3, 4); // All 5 LEDs
        }
        
        String room = target.toString().toLowerCase().replace(" ", "_").replace("-", "_");
        
        // Map room names to LED indices (matching your embedded config)
        return switch (room) {
            case "living_room", "living" -> List.of(0);
            case "bedroom" -> List.of(1);
            case "kitchen" -> List.of(2);
            case "bathroom", "bath" -> List.of(3);
            case "hallway" -> List.of(4);
            default -> List.of(0, 1, 2, 3, 4); // Default to all
        };
    }

    /**
     * Parse an RGB string like "[255, 0, 0]" into a list of integers.
     */
    private List<Integer> parseRgbString(String rgbStr) {
        if (rgbStr == null) return null;
        try {
            // Remove brackets and split
            String cleaned = rgbStr.replaceAll("[\\[\\]\\s]", "");
            String[] parts = cleaned.split(",");
            if (parts.length >= 3) {
                return List.of(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
                );
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse RGB string: {}", rgbStr);
        }
        return null;
    }

    /**
     * Convert a color name to RGB values.
     */
    private List<Integer> colorNameToRgb(String colorName) {
        if (colorName == null) return null;
        String color = colorName.toLowerCase().trim();
        return switch (color) {
            case "red" -> List.of(255, 0, 0);
            case "green" -> List.of(0, 255, 0);
            case "blue" -> List.of(0, 0, 255);
            case "yellow" -> List.of(255, 255, 0);
            case "orange" -> List.of(255, 165, 0);
            case "purple", "violet" -> List.of(128, 0, 128);
            case "pink" -> List.of(255, 105, 180);
            case "cyan", "aqua" -> List.of(0, 255, 255);
            case "magenta" -> List.of(255, 0, 255);
            case "white" -> List.of(255, 255, 255);
            case "warm white", "warm" -> List.of(255, 244, 229);
            case "cool white", "cool" -> List.of(200, 220, 255);
            case "gold" -> List.of(255, 215, 0);
            case "lime" -> List.of(0, 255, 0);
            case "coral" -> List.of(255, 127, 80);
            case "salmon" -> List.of(250, 128, 114);
            case "teal" -> List.of(0, 128, 128);
            case "indigo" -> List.of(75, 0, 130);
            case "turquoise" -> List.of(64, 224, 208);
            default -> null;
        };
    }

    /**
     * Create a schedule from parsed command.
     */
    private Schedule createScheduleFromParsed(ParsedCommand parsed, User user) {
        ScheduleConfig scheduleConfig = parsed.getSchedule();
        
        String triggerType = scheduleConfig.getTime() != null ? "time" : "sun";
        
        Map<String, Object> triggerConfigMap = new HashMap<>();
        if ("time".equals(triggerType)) {
            triggerConfigMap.put("at", scheduleConfig.getTime() + ":00");
            if (scheduleConfig.getRecurrence() != null) {
                if ("weekdays".equals(scheduleConfig.getRecurrence())) {
                    triggerConfigMap.put("weekdays", List.of("mon", "tue", "wed", "thu", "fri"));
                } else if ("weekends".equals(scheduleConfig.getRecurrence())) {
                    triggerConfigMap.put("weekdays", List.of("sat", "sun"));
                }
            }
        } else {
            triggerConfigMap.put("event", scheduleConfig.getTrigger());
            if (scheduleConfig.getOffsetMinutes() != null) {
                triggerConfigMap.put("offset_minutes", scheduleConfig.getOffsetMinutes());
            }
        }
        
        // Build action
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        
        // Set action type based on intent
        if (parsed.getIntent() != null && parsed.getIntent().startsWith("scene.")) {
            action.put("type", "scene");
            action.put("scene", parsed.getScene());
            // For scenes, also include target if specified (so it applies to specific room)
            if (parsed.getTarget() != null) {
                action.put("target", parsed.getTarget());
            }
        } else {
            action.put("type", "light");
            action.put("intent", parsed.getIntent());
            action.put("target", parsed.getTarget());
            if (parsed.getParams() != null) {
                action.put("params", parsed.getParams());
            }
        }
        actions.add(action);
        
        // Generate name
        String name = generateScheduleName(parsed);
        
        Schedule schedule = Schedule.builder()
            .name(name)
            .description("Created via natural language: " + parsed.getIntent())
            .enabled(true)
            .triggerType(triggerType)
            .triggerConfig(triggerConfigMap)
            .actions(actions)
            .createdBy(user)
            .build();
        
        return scheduleRepository.save(schedule);
    }

    /**
     * Generate a name for a schedule.
     */
    private String generateScheduleName(ParsedCommand parsed) {
        String intent = parsed.getIntent().replace("light.", "").replace("scene.", "");
        String target = parsed.getTarget() != null ? parsed.getTarget().toString() : "all";
        ScheduleConfig schedule = parsed.getSchedule();
        
        String timeStr = schedule.getTime() != null ? schedule.getTime() : schedule.getTrigger();
        
        return String.format("%s %s at %s", 
            intent.substring(0, 1).toUpperCase() + intent.substring(1),
            target.replace("_", " "),
            timeStr
        );
    }

    /**
     * Save command to history.
     */
    private void saveCommandHistory(NlpCommandDto commandDto, User user) {
        try {
            Map<String, Object> parsedMap = objectMapper.convertValue(commandDto.getParsed(), new TypeReference<>() {});
            
            NlpCommand nlpCommand = NlpCommand.builder()
                .rawInput(commandDto.getText())
                .parsedJson(parsedMap)
                .executed(commandDto.getExecuted())
                .executionResult(commandDto.getResult())
                .isScheduled(commandDto.getIsScheduled())
                .user(user)
                .build();
            
            nlpCommandRepository.save(nlpCommand);
        } catch (Exception e) {
            log.error("Error saving command history: {}", e.getMessage());
        }
    }

    /**
     * Check for schedule conflicts for a parsed command.
     */
    private ConflictAnalysisDto checkForScheduleConflicts(ParsedCommand parsed) {
        try {
            // Build a temporary schedule to check for conflicts
            Schedule tempSchedule = buildTemporarySchedule(parsed);
            
            ConflictAnalysisResult result = conflictService.detectConflicts(tempSchedule);
            
            if (!result.hasConflicts()) {
                return null;
            }
            
            // Convert to DTO
            List<ConflictAnalysisDto.ConflictDto> conflictDtos = result.conflicts().stream()
                .map(this::toConflictDto)
                .toList();
            
            return ConflictAnalysisDto.builder()
                .hasConflicts(true)
                .summary(result.summary())
                .conflicts(conflictDtos)
                .build();
                
        } catch (Exception e) {
            log.warn("Error checking for schedule conflicts: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build a temporary schedule from parsed command for conflict detection.
     */
    private Schedule buildTemporarySchedule(ParsedCommand parsed) {
        ScheduleConfig scheduleConfig = parsed.getSchedule();
        
        Map<String, Object> triggerConfig = new HashMap<>();
        String triggerType = "time";
        
        if (scheduleConfig.getTime() != null) {
            triggerConfig.put("at", scheduleConfig.getTime() + ":00");
            if (scheduleConfig.getRecurrence() != null) {
                if ("weekdays".equals(scheduleConfig.getRecurrence().toString())) {
                    triggerConfig.put("weekdays", List.of("mon", "tue", "wed", "thu", "fri"));
                } else if ("weekends".equals(scheduleConfig.getRecurrence().toString())) {
                    triggerConfig.put("weekdays", List.of("sat", "sun"));
                }
            }
        } else if (scheduleConfig.getTrigger() != null) {
            triggerType = "sun";
            triggerConfig.put("event", scheduleConfig.getTrigger());
        }
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        
        if (parsed.getIntent() != null && parsed.getIntent().startsWith("scene.")) {
            action.put("type", "scene");
            action.put("scene", parsed.getScene());
        } else {
            action.put("type", "light");
            action.put("intent", parsed.getIntent());
        }
        action.put("target", parsed.getTarget());
        if (parsed.getParams() != null) {
            action.put("params", parsed.getParams());
        }
        actions.add(action);
        
        return Schedule.builder()
            .id(UUID.randomUUID()) // Temp ID
            .name("New Schedule")
            .enabled(true)
            .triggerType(triggerType)
            .triggerConfig(triggerConfig)
            .actions(actions)
            .build();
    }

    /**
     * Convert conflict to DTO.
     */
    private ConflictAnalysisDto.ConflictDto toConflictDto(ScheduleConflict conflict) {
        List<ConflictAnalysisDto.ResolutionDto> resolutionDtos = conflict.resolutions().stream()
            .map(this::toResolutionDto)
            .toList();
        
        return ConflictAnalysisDto.ConflictDto.builder()
            .scheduleId1(conflict.scheduleId1().toString())
            .scheduleName1(conflict.scheduleName1())
            .scheduleId2(conflict.scheduleId2().toString())
            .scheduleName2(conflict.scheduleName2())
            .conflictType(conflict.conflictType())
            .description(conflict.description())
            .severity(conflict.severity())
            .resolutions(resolutionDtos)
            .build();
    }

    /**
     * Convert resolution to DTO.
     */
    private ConflictAnalysisDto.ResolutionDto toResolutionDto(ConflictResolution resolution) {
        return ConflictAnalysisDto.ResolutionDto.builder()
            .id(resolution.id())
            .description(resolution.description())
            .action(resolution.action())
            .changes(resolution.changes())
            .build();
    }

    /**
     * Apply a conflict resolution.
     */
    public String applyConflictResolution(UUID scheduleId, String resolutionId, Map<String, Object> params) {
        return conflictService.applyResolution(scheduleId, resolutionId, params);
    }
}

