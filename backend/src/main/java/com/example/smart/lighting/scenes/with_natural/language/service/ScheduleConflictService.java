package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for detecting and resolving schedule conflicts using AI.
 * Analyzes schedules for timing conflicts, contradictory actions, and overlapping effects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleConflictService {

    private final ScheduleRepository scheduleRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    private static final int CONFLICT_WINDOW_MINUTES = 30; // Consider schedules within 30 min as potential conflicts

    /**
     * Represents a detected conflict between schedules.
     */
    public record ScheduleConflict(
        UUID scheduleId1,
        String scheduleName1,
        UUID scheduleId2,
        String scheduleName2,
        String conflictType,
        String description,
        String severity, // "high", "medium", "low"
        List<ConflictResolution> resolutions
    ) {}

    /**
     * Represents a possible resolution for a conflict.
     */
    public record ConflictResolution(
        String id,
        String description,
        String action, // "adjust_time", "merge", "prioritize", "disable", "delete"
        Map<String, Object> changes
    ) {}

    /**
     * Result of conflict analysis.
     */
    public record ConflictAnalysisResult(
        boolean hasConflicts,
        List<ScheduleConflict> conflicts,
        String summary
    ) {}

    /**
     * Detect conflicts for a new or updated schedule.
     */
    public ConflictAnalysisResult detectConflicts(Schedule newSchedule) {
        List<Schedule> existingSchedules = scheduleRepository.findByEnabledTrue();

        // Filter out the schedule itself if it's an update
        existingSchedules = existingSchedules.stream()
            .filter(s -> !s.getId().equals(newSchedule.getId()))
            .toList();

        if (existingSchedules.isEmpty()) {
            return new ConflictAnalysisResult(false, List.of(), "No existing schedules to conflict with.");
        }

        // Find potential conflicts based on time proximity
        List<ScheduleConflict> conflicts = new ArrayList<>();

        for (Schedule existing : existingSchedules) {
            Optional<ScheduleConflict> conflict = checkForConflict(newSchedule, existing);
            conflict.ifPresent(conflicts::add);
        }

        if (conflicts.isEmpty()) {
            return new ConflictAnalysisResult(false, List.of(), "No conflicts detected.");
        }

        // Use AI to enhance conflict analysis and generate smart resolutions
        return enhanceWithAI(newSchedule, conflicts);
    }

    /**
     * Check if two schedules conflict.
     */
    private Optional<ScheduleConflict> checkForConflict(Schedule schedule1, Schedule schedule2) {
        // Only check time-based schedules for now
        if (!"time".equals(schedule1.getTriggerType()) || !"time".equals(schedule2.getTriggerType())) {
            return Optional.empty();
        }

        Map<String, Object> config1 = schedule1.getTriggerConfig();
        Map<String, Object> config2 = schedule2.getTriggerConfig();

        String time1Str = (String) config1.get("at");
        String time2Str = (String) config2.get("at");

        if (time1Str == null || time2Str == null) {
            return Optional.empty();
        }

        LocalTime time1 = parseTime(time1Str);
        LocalTime time2 = parseTime(time2Str);

        if (time1 == null || time2 == null) {
            return Optional.empty();
        }

        // Check if times are within the conflict window
        long minutesDiff = Math.abs(time1.toSecondOfDay() - time2.toSecondOfDay()) / 60;
        if (minutesDiff > CONFLICT_WINDOW_MINUTES) {
            return Optional.empty();
        }

        // Check if days overlap
        if (!daysOverlap(config1, config2)) {
            return Optional.empty();
        }

        // Check if targets overlap
        String target1 = getTarget(schedule1);
        String target2 = getTarget(schedule2);

        if (!targetsOverlap(target1, target2)) {
            return Optional.empty();
        }

        // Determine conflict type and severity
        String conflictType = determineConflictType(schedule1, schedule2);
        String severity = determineSeverity(schedule1, schedule2, minutesDiff);
        String description = generateConflictDescription(schedule1, schedule2, minutesDiff, target1, target2);

        // Generate basic resolutions
        List<ConflictResolution> resolutions = generateBasicResolutions(schedule1, schedule2, minutesDiff);

        return Optional.of(new ScheduleConflict(
            schedule1.getId(),
            schedule1.getName(),
            schedule2.getId(),
            schedule2.getName(),
            conflictType,
            description,
            severity,
            resolutions
        ));
    }

    private LocalTime parseTime(String timeStr) {
        try {
            if (timeStr.length() == 5) {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    private boolean daysOverlap(Map<String, Object> config1, Map<String, Object> config2) {
        Object days1 = config1.get("weekdays");
        Object days2 = config2.get("weekdays");

        // If either has no specific days, assume every day (overlaps with anything)
        if (days1 == null || days2 == null) {
            return true;
        }

        if (days1 instanceof List<?> list1 && days2 instanceof List<?> list2) {
            Set<String> set1 = new HashSet<>();
            list1.forEach(d -> set1.add(d.toString().toLowerCase()));

            for (Object d : list2) {
                if (set1.contains(d.toString().toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private String getTarget(Schedule schedule) {
        List<Map<String, Object>> actions = schedule.getActions();
        if (actions == null || actions.isEmpty()) {
            return "all";
        }
        Object target = actions.get(0).get("target");
        return target != null ? target.toString() : "all";
    }

    private boolean targetsOverlap(String target1, String target2) {
        if ("all".equalsIgnoreCase(target1) || "all".equalsIgnoreCase(target2)) {
            return true;
        }
        return target1.equalsIgnoreCase(target2);
    }

    private String determineConflictType(Schedule s1, Schedule s2) {
        String intent1 = getIntent(s1);
        String intent2 = getIntent(s2);

        // Direct contradiction
        if (("light.on".equals(intent1) && "light.off".equals(intent2)) ||
            ("light.off".equals(intent1) && "light.on".equals(intent2))) {
            return "contradiction";
        }

        // Same action type at similar times
        if (intent1.equals(intent2)) {
            return "duplicate";
        }

        // Scene conflicts
        if (intent1.startsWith("scene.") && intent2.startsWith("scene.")) {
            return "scene_overlap";
        }

        // Different brightness levels
        if ("light.brightness".equals(intent1) && "light.brightness".equals(intent2)) {
            return "brightness_conflict";
        }

        return "timing_overlap";
    }

    private String getIntent(Schedule schedule) {
        List<Map<String, Object>> actions = schedule.getActions();
        if (actions == null || actions.isEmpty()) {
            return "unknown";
        }
        Map<String, Object> action = actions.get(0);
        String type = (String) action.get("type");
        if ("scene".equals(type)) {
            return "scene.apply";
        }
        return (String) action.getOrDefault("intent", "unknown");
    }

    private String determineSeverity(Schedule s1, Schedule s2, long minutesDiff) {
        String type = determineConflictType(s1, s2);

        if ("contradiction".equals(type)) {
            return minutesDiff == 0 ? "high" : "medium";
        }
        if ("duplicate".equals(type) && minutesDiff == 0) {
            return "high";
        }
        if (minutesDiff <= 5) {
            return "medium";
        }
        return "low";
    }

    private String generateConflictDescription(Schedule s1, Schedule s2, long minutesDiff, String target1, String target2) {
        String type = determineConflictType(s1, s2);
        String timeDesc = minutesDiff == 0 ? "at the exact same time" :
            String.format("%d minutes apart", minutesDiff);

        String targetDesc = target1.equals(target2) ?
            String.format("the %s", target1.replace("_", " ")) :
            "overlapping areas (including 'all' rooms)";

        return switch (type) {
            case "contradiction" -> String.format(
                "These schedules will turn lights ON and OFF %s for %s. This will cause flickering or unexpected behavior.",
                timeDesc, targetDesc);
            case "duplicate" -> String.format(
                "Both schedules perform the same action %s for %s. One may be redundant.",
                timeDesc, targetDesc);
            case "scene_overlap" -> String.format(
                "Two different scenes will be applied %s to %s. The second scene will immediately override the first.",
                timeDesc, targetDesc);
            case "brightness_conflict" -> String.format(
                "Different brightness levels will be set %s for %s. This may cause visible flickering.",
                timeDesc, targetDesc);
            default -> String.format(
                "These schedules trigger %s and affect %s. They may interfere with each other.",
                timeDesc, targetDesc);
        };
    }

    private List<ConflictResolution> generateBasicResolutions(Schedule s1, Schedule s2, long minutesDiff) {
        List<ConflictResolution> resolutions = new ArrayList<>();
        String type = determineConflictType(s1, s2);

        // Resolution 1: Adjust timing
        if (minutesDiff < 15) {
            Map<String, Object> changes = new HashMap<>();
            changes.put("schedule_id", s1.getId().toString());
            changes.put("new_time", adjustTime(s1, 15));
            resolutions.add(new ConflictResolution(
                "adjust_new",
                String.format("Move '%s' 15 minutes later to avoid overlap", s1.getName()),
                "adjust_time",
                changes
            ));
        }

        // Resolution 2: Prioritize existing
        resolutions.add(new ConflictResolution(
            "prioritize_existing",
            String.format("Keep '%s' and disable the new schedule", s2.getName()),
            "disable",
            Map.of("schedule_id", s1.getId().toString())
        ));

        // Resolution 3: Replace existing
        resolutions.add(new ConflictResolution(
            "replace_existing",
            String.format("Replace '%s' with the new schedule", s2.getName()),
            "delete",
            Map.of("schedule_id", s2.getId().toString())
        ));

        // Type-specific resolutions
        if ("contradiction".equals(type)) {
            // Merge into a toggle
            resolutions.add(new ConflictResolution(
                "merge_toggle",
                "Create a single schedule that turns lights on, then off after a duration",
                "merge",
                Map.of(
                    "schedule_id_1", s1.getId().toString(),
                    "schedule_id_2", s2.getId().toString(),
                    "suggested_duration", "30"
                )
            ));
        }

        return resolutions;
    }

    private String adjustTime(Schedule schedule, int minutesToAdd) {
        Map<String, Object> config = schedule.getTriggerConfig();
        String timeStr = (String) config.get("at");
        LocalTime time = parseTime(timeStr);
        if (time != null) {
            return time.plusMinutes(minutesToAdd).format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return timeStr;
    }

    /**
     * Use AI to enhance conflict analysis with smarter resolutions.
     */
    private ConflictAnalysisResult enhanceWithAI(Schedule newSchedule, List<ScheduleConflict> basicConflicts) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            // Return basic analysis if no API key
            String summary = String.format("Found %d potential conflict(s). Review the suggested resolutions.", basicConflicts.size());
            return new ConflictAnalysisResult(true, basicConflicts, summary);
        }

        try {
            String prompt = buildAIPrompt(newSchedule, basicConflicts);
            String aiResponse = callOpenAI(prompt);
            return parseAIResponse(aiResponse, basicConflicts);
        } catch (Exception e) {
            log.warn("AI enhancement failed, using basic analysis: {}", e.getMessage());
            String summary = String.format("Found %d potential conflict(s). Review the suggested resolutions.", basicConflicts.size());
            return new ConflictAnalysisResult(true, basicConflicts, summary);
        }
    }

    private String buildAIPrompt(Schedule newSchedule, List<ScheduleConflict> conflicts) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            You are a smart home schedule conflict analyzer. Analyze these lighting schedule conflicts and provide enhanced resolutions.

            New Schedule:
            """);
        sb.append(scheduleToString(newSchedule));
        sb.append("\n\nDetected Conflicts:\n");

        for (int i = 0; i < conflicts.size(); i++) {
            ScheduleConflict c = conflicts.get(i);
            sb.append(String.format("%d. %s vs %s\n", i + 1, c.scheduleName1(), c.scheduleName2()));
            sb.append(String.format("   Type: %s, Severity: %s\n", c.conflictType(), c.severity()));
            sb.append(String.format("   Description: %s\n", c.description()));
        }

        sb.append("""

            Provide a JSON response with:
            {
              "summary": "A user-friendly summary of all conflicts (1-2 sentences)",
              "enhanced_resolutions": [
                {
                  "conflict_index": 0,
                  "best_resolution": "The ID of the best resolution from the existing options",
                  "reasoning": "Why this is the best choice",
                  "additional_suggestion": "Any smart alternative not in the basic options (optional)"
                }
              ],
              "user_tip": "A helpful tip for avoiding future conflicts"
            }

            Be practical and consider real-world lighting usage patterns. For example:
            - Morning routines need gradual wake-up lighting
            - Bedtime should have dimming sequences
            - Contradicting on/off commands are usually user errors
            - Similar scenes close together are often intended as backups
            """);

        return sb.toString();
    }

    private String scheduleToString(Schedule schedule) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("name", schedule.getName());
            map.put("trigger", schedule.getTriggerConfig());
            map.put("actions", schedule.getActions());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return schedule.getName();
        }
    }

    private String callOpenAI(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = Map.of(
            "model", openaiModel,
            "messages", List.of(
                Map.of("role", "system", "content", "You are a smart home assistant specializing in schedule optimization."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.openai.com/v1/chat/completions",
            HttpMethod.POST,
            request,
            String.class
        );

        // Extract content from response
        JsonNode root = null;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private ConflictAnalysisResult parseAIResponse(String aiResponse, List<ScheduleConflict> basicConflicts) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String summary = root.path("summary").asText(
                String.format("Found %d potential conflict(s).", basicConflicts.size())
            );

            String userTip = root.path("user_tip").asText("");
            if (!userTip.isEmpty()) {
                summary += " Tip: " + userTip;
            }

            // Enhance conflicts with AI suggestions
            JsonNode enhancedResolutions = root.path("enhanced_resolutions");
            List<ScheduleConflict> enhancedConflicts = new ArrayList<>();

            for (int i = 0; i < basicConflicts.size(); i++) {
                ScheduleConflict original = basicConflicts.get(i);

                // Find AI enhancement for this conflict
                String additionalSuggestion = null;
                String bestResolutionId = null;

                if (enhancedResolutions.isArray()) {
                    for (JsonNode enhancement : enhancedResolutions) {
                        if (enhancement.path("conflict_index").asInt(-1) == i) {
                            additionalSuggestion = enhancement.path("additional_suggestion").asText(null);
                            bestResolutionId = enhancement.path("best_resolution").asText(null);
                            break;
                        }
                    }
                }

                // Add AI-suggested resolution if present
                List<ConflictResolution> enhancedResList = new ArrayList<>(original.resolutions());
                if (additionalSuggestion != null && !additionalSuggestion.isBlank()) {
                    enhancedResList.add(0, new ConflictResolution(
                        "ai_suggested",
                        additionalSuggestion,
                        "custom",
                        Map.of("ai_generated", true)
                    ));
                }

                // Reorder to put best resolution first
                if (bestResolutionId != null) {
                    String finalBestId = bestResolutionId;
                    enhancedResList.sort((a, b) -> {
                        if (a.id().equals(finalBestId)) return -1;
                        if (b.id().equals(finalBestId)) return 1;
                        if (a.id().equals("ai_suggested")) return -1;
                        if (b.id().equals("ai_suggested")) return 1;
                        return 0;
                    });
                }

                enhancedConflicts.add(new ScheduleConflict(
                    original.scheduleId1(),
                    original.scheduleName1(),
                    original.scheduleId2(),
                    original.scheduleName2(),
                    original.conflictType(),
                    original.description(),
                    original.severity(),
                    enhancedResList
                ));
            }

            return new ConflictAnalysisResult(true, enhancedConflicts, summary);

        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
            String summary = String.format("Found %d potential conflict(s). Review the suggested resolutions.", basicConflicts.size());
            return new ConflictAnalysisResult(true, basicConflicts, summary);
        }
    }

    /**
     * Apply a conflict resolution.
     */
    public String applyResolution(UUID conflictScheduleId, String resolutionId, Map<String, Object> params) {
        switch (resolutionId) {
            case "adjust_new", "adjust_time" -> {
                String newTime = (String) params.get("new_time");
                if (newTime != null) {
                    Schedule schedule = scheduleRepository.findById(conflictScheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
                    Map<String, Object> config = new HashMap<>(schedule.getTriggerConfig());
                    config.put("at", newTime + ":00");
                    schedule.setTriggerConfig(config);
                    scheduleRepository.save(schedule);
                    return "Schedule time adjusted to " + newTime;
                }
            }
            case "disable", "prioritize_existing" -> {
                String scheduleIdStr = (String) params.get("schedule_id");
                if (scheduleIdStr != null) {
                    UUID scheduleId = UUID.fromString(scheduleIdStr);
                    Schedule schedule = scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
                    schedule.setEnabled(false);
                    scheduleRepository.save(schedule);
                    return "Schedule '" + schedule.getName() + "' has been disabled";
                }
            }
            case "delete", "replace_existing" -> {
                String scheduleIdStr = (String) params.get("schedule_id");
                if (scheduleIdStr != null) {
                    UUID scheduleId = UUID.fromString(scheduleIdStr);
                    Schedule schedule = scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
                    String name = schedule.getName();
                    scheduleRepository.delete(schedule);
                    return "Schedule '" + name + "' has been deleted";
                }
            }
            case "merge_toggle" -> {
                // More complex merge logic would go here
                return "Merge functionality requires manual configuration. Please edit the schedules manually.";
            }
            default -> {
                return "Resolution applied (custom action)";
            }
        }
        return "Resolution could not be applied";
    }
}
