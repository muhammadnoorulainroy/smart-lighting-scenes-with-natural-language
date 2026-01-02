package com.example.smart.lighting.scenes.with_natural.language.service.nlp;

import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ParsedCommand;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ScheduleConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Parses and validates NLP commands from OpenAI responses.
 *

 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NlpCommandParser {

    private final ObjectMapper objectMapper;
    private final NlpColorUtils colorUtils;

    private static final List<String> AVAILABLE_ROOMS = List.of(
        "bedroom", "living_room", "living-room", "kitchen", "bathroom", "hallway", "all"
    );

    /**
     * Parse the JSON response from OpenAI into a ParsedCommand.
     *
     * @param response the OpenAI response
     * @return parsed command or null if parsing failed
     */
    @SuppressWarnings("unchecked")
    public ParsedCommand parseOpenAIResponse(String response) {
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }

            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            ParsedCommand.ParsedCommandBuilder builder = ParsedCommand.builder()
                .intent((String) map.get("intent"))
                .target(map.get("target"))
                .scene((String) map.get("scene"))
                .confidence(map.get("confidence") != null
                    ? ((Number) map.get("confidence")).doubleValue() : 0.8);

            if (map.get("params") != null) {
                builder.params((Map<String, Object>) map.get("params"));
            }

            if (map.get("schedule") != null) {
                Map<String, Object> scheduleMap = (Map<String, Object>) map.get("schedule");
                ScheduleConfig schedule = ScheduleConfig.builder()
                    .time((String) scheduleMap.get("time"))
                    .trigger((String) scheduleMap.get("trigger"))
                    .offsetMinutes(scheduleMap.get("offsetMinutes") != null
                        ? ((Number) scheduleMap.get("offsetMinutes")).intValue() : null)
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
     * Validate the parsed command and return an error message if invalid.
     *
     * @param parsed the parsed command
     * @return error message or null if valid
     */
    public String validateParsedCommand(ParsedCommand parsed) {
        if (parsed.getIntent() == null || parsed.getIntent().isBlank()) {
            return "Could not determine what action to take. "
                + "Please specify what you want to do with the lights.";
        }

        if (parsed.getIntent().startsWith("light.")) {
            Object target = parsed.getTarget();
            if (target == null) {
                return "Please specify which room or 'all lights'.";
            }

            String targetStr = target.toString().toLowerCase()
                .replace(" ", "_").replace("-", "_");
            if (!targetStr.equals("all") && !AVAILABLE_ROOMS.contains(targetStr)) {
                return "Unknown room: '" + target
                    + "'. Available rooms: bedroom, living room, kitchen, bathroom, hallway, or 'all'.";
            }
        }

        String brightnessError = validateBrightness(parsed);
        if (brightnessError != null) {
            return brightnessError;
        }

        String colorError = validateColor(parsed);
        if (colorError != null) {
            return colorError;
        }

        if ("scene.apply".equals(parsed.getIntent())) {
            if (parsed.getScene() == null || parsed.getScene().isBlank()) {
                return "Please specify which scene to apply.";
            }
        }

        if (parsed.getConfidence() != null && parsed.getConfidence() < 0.5) {
            return "I'm not confident I understood correctly. Please try rephrasing your command.";
        }

        return null;
    }

    private String validateBrightness(ParsedCommand parsed) {
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
        return null;
    }

    @SuppressWarnings("unchecked")
    private String validateColor(ParsedCommand parsed) {
        if (!"light.color".equals(parsed.getIntent())) {
            return null;
        }

        Map<String, Object> params = parsed.getParams();
        if (params == null) {
            return "Please specify a color.";
        }

        Object rgb = params.get("rgb");
        Object color = params.get("color");

        if (rgb instanceof List<?> rgbList && rgbList.size() >= 3) {
            return null; // Valid RGB
        }

        if (rgb instanceof String rgbStr && rgbStr.startsWith("[")) {
            List<Integer> rgbValues = colorUtils.parseRgbString(rgbStr);
            if (rgbValues != null) {
                params.put("rgb", rgbValues);
                return null;
            }
        }

        if (color != null) {
            String colorStr = color.toString();
            if (colorStr.startsWith("[")) {
                List<Integer> rgbValues = colorUtils.parseRgbString(colorStr);
                if (rgbValues != null) {
                    params.put("rgb", rgbValues);
                    return null;
                }
            } else {
                List<Integer> rgbValues = colorUtils.colorNameToRgb(colorStr);
                if (rgbValues != null) {
                    params.put("rgb", rgbValues);
                    return null;
                }
                return "Unknown color: '" + color
                    + "'. Try 'red', 'blue', 'green', 'yellow', 'purple', 'orange', 'white', or 'pink'.";
            }
        }

        if (rgb == null) {
            return "Please specify a color (e.g., 'red', 'blue', or RGB values).";
        }

        return null;
    }

    /**
     * Generate a human-readable preview of the command.
     *
     * @param parsed the parsed command
     * @return preview text
     */
    @SuppressWarnings("unchecked")
    public String generatePreview(ParsedCommand parsed) {
        StringBuilder preview = new StringBuilder();

        String intent = parsed.getIntent();
        Object target = parsed.getTarget();
        String targetStr = target != null ? target.toString() : "all";
        targetStr = targetStr.replace("_", " ");

        switch (intent) {
            case "light.on" -> preview.append("Turn ON ").append(targetStr).append(" lights");
            case "light.off" -> preview.append("Turn OFF ").append(targetStr).append(" lights");
            case "light.brightness" -> {
                int brightness = ((Number) parsed.getParams().get("brightness")).intValue();
                preview.append("Set ").append(targetStr).append(" lights to ")
                    .append(brightness).append("% brightness");
            }
            case "light.color" -> appendColorPreview(preview, parsed, targetStr);
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
            case "scene.create" ->
                preview.append("Create new scene '").append(parsed.getScene()).append("'");
            default -> preview.append("Execute: ").append(intent);
        }

        appendSchedulePreview(preview, parsed.getSchedule());

        return preview.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendColorPreview(StringBuilder preview, ParsedCommand parsed, String targetStr) {
        Object rgbObj = parsed.getParams() != null ? parsed.getParams().get("rgb") : null;
        if (rgbObj instanceof List<?> rgb && rgb.size() >= 3) {
            preview.append("Set ").append(targetStr).append(" lights to color RGB(")
                .append(rgb.get(0)).append(", ").append(rgb.get(1))
                .append(", ").append(rgb.get(2)).append(")");
        } else {
            Object color = parsed.getParams() != null ? parsed.getParams().get("color") : "custom";
            preview.append("Set ").append(targetStr).append(" lights to ")
                .append(color).append(" color");
        }
    }

    private void appendSchedulePreview(StringBuilder preview, ScheduleConfig schedule) {
        if (schedule == null) {
            return;
        }

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
}

