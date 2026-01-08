package com.example.smart.lighting.scenes.with_natural.language.service.nlp;

import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ParsedCommand;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ScheduleConfig;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds Schedule entities from parsed NLP commands.
 *

 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NlpScheduleBuilder {

    private final ScheduleRepository scheduleRepository;
    private final WebSocketEventService webSocketEventService;

    /**
     * Create a schedule from a parsed command.
     *
     * @param parsed the parsed command
     * @param user the user creating the schedule
     * @return the saved schedule
     */
    public Schedule createScheduleFromParsed(ParsedCommand parsed, User user) {
        ScheduleConfig scheduleConfig = parsed.getSchedule();

        String triggerType = scheduleConfig.getTime() != null ? "time" : "sun";

        Map<String, Object> triggerConfigMap = new HashMap<>();
        if ("time".equals(triggerType)) {
            // Normalize time to HH:mm:ss format
            String timeStr = scheduleConfig.getTime();
            if (timeStr != null && timeStr.length() == 5) {
                timeStr = timeStr + ":00"; // 22:00 -> 22:00:00
            }
            triggerConfigMap.put("at", timeStr);
            log.info("Creating schedule with trigger time: {}", timeStr);
            if (scheduleConfig.getRecurrence() != null) {
                List<String> weekdays = parseRecurrence(scheduleConfig.getRecurrence());
                if (weekdays != null && !weekdays.isEmpty()) {
                    triggerConfigMap.put("weekdays", weekdays);
                }
            }
        } else {
            triggerConfigMap.put("event", scheduleConfig.getTrigger());
            if (scheduleConfig.getOffsetMinutes() != null) {
                triggerConfigMap.put("offset_minutes", scheduleConfig.getOffsetMinutes());
            }
        }

        List<Map<String, Object>> actions = buildActions(parsed);
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

        schedule = scheduleRepository.save(schedule);

        // Broadcast WebSocket event for real-time sync
        webSocketEventService.broadcastScheduleCreated(schedule.getId(), schedule.getName());

        return schedule;
    }

    private List<Map<String, Object>> buildActions(ParsedCommand parsed) {
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();

        if (parsed.getIntent() != null && parsed.getIntent().startsWith("scene.")) {
            action.put("type", "scene");
            action.put("scene", parsed.getScene());
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

        return actions;
    }

    private String generateScheduleName(ParsedCommand parsed) {
        String intent = parsed.getIntent().replace("light.", "").replace("scene.", "");
        String target = parsed.getTarget() != null ? parsed.getTarget().toString() : "all";
        ScheduleConfig schedule = parsed.getSchedule();

        String timeStr = schedule.getTime() != null ? schedule.getTime() : schedule.getTrigger();

        // Add recurrence info to name
        String recurrenceStr = "";
        if (schedule.getRecurrence() != null) {
            List<String> days = parseRecurrence(schedule.getRecurrence());
            if (days != null && !days.isEmpty() && days.size() < 7) {
                recurrenceStr = " on " + String.join(", ", days);
            }
        }

        return String.format("%s %s at %s%s",
            intent.substring(0, 1).toUpperCase() + intent.substring(1),
            target.replace("_", " "),
            timeStr,
            recurrenceStr
        );
    }

    /**
     * Parse recurrence configuration into a list of day abbreviations.
     *
     * @param recurrence the recurrence value (String or List)
     * @return list of day abbreviations, or null for daily
     */
    @SuppressWarnings("unchecked")
    private List<String> parseRecurrence(Object recurrence) {
        if (recurrence == null) {
            return null;
        }

        // Handle list of days directly from LLM
        if (recurrence instanceof List<?>) {
            List<String> days = ((List<?>) recurrence).stream()
                .map(Object::toString)
                .map(String::toLowerCase)
                .toList();
            log.debug("Parsed recurrence list: {}", days);
            return days;
        }

        // Handle string values
        String recurrenceStr = recurrence.toString().toLowerCase();
        return switch (recurrenceStr) {
            case "weekdays" -> List.of("mon", "tue", "wed", "thu", "fri");
            case "weekends" -> List.of("sat", "sun");
            case "daily", "everyday", "every day" ->
                List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun");
            case "once" -> null; // One-time schedule, no weekdays filter
            default -> {
                log.warn("Unknown recurrence value: {}", recurrenceStr);
                yield null;
            }
        };
    }

    /**
     * Build a temporary schedule from parsed command for conflict detection.
     *
     * @param parsed the parsed command
     * @return temporary schedule (not persisted)
     */
    public Schedule buildTemporarySchedule(ParsedCommand parsed) {
        ScheduleConfig scheduleConfig = parsed.getSchedule();

        Map<String, Object> triggerConfig = new HashMap<>();
        String triggerType = "time";

        if (scheduleConfig.getTime() != null) {
            // Normalize time to HH:mm:ss format
            String timeStr = scheduleConfig.getTime();
            if (timeStr != null && timeStr.length() == 5) {
                timeStr = timeStr + ":00";
            }
            triggerConfig.put("at", timeStr);
            if (scheduleConfig.getRecurrence() != null) {
                List<String> weekdays = parseRecurrence(scheduleConfig.getRecurrence());
                if (weekdays != null && !weekdays.isEmpty()) {
                    triggerConfig.put("weekdays", weekdays);
                }
            }
        } else if (scheduleConfig.getTrigger() != null) {
            triggerType = "sun";
            triggerConfig.put("event", scheduleConfig.getTrigger());
        }

        List<Map<String, Object>> actions = buildActions(parsed);

        return Schedule.builder()
            .id(UUID.randomUUID())
            .name("New Schedule")
            .enabled(true)
            .triggerType(triggerType)
            .triggerConfig(triggerConfig)
            .actions(actions)
            .build();
    }
}
