package com.example.smart.lighting.scenes.with_natural.language.service.nlp;

import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ParsedCommand;
import com.example.smart.lighting.scenes.with_natural.language.dto.NlpCommandDto.ScheduleConfig;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
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

        return scheduleRepository.save(schedule);
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

        return String.format("%s %s at %s",
            intent.substring(0, 1).toUpperCase() + intent.substring(1),
            target.replace("_", " "),
            timeStr
        );
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
            triggerConfig.put("at", scheduleConfig.getTime() + ":00");
            if (scheduleConfig.getRecurrence() != null) {
                String recurrence = scheduleConfig.getRecurrence().toString();
                if ("weekdays".equals(recurrence)) {
                    triggerConfig.put("weekdays", List.of("mon", "tue", "wed", "thu", "fri"));
                } else if ("weekends".equals(recurrence)) {
                    triggerConfig.put("weekdays", List.of("sat", "sun"));
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

