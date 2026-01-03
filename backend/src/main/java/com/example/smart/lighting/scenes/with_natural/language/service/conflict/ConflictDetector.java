package com.example.smart.lighting.scenes.with_natural.language.service.conflict;

import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Detects conflicts between schedules based on timing and targets.
 *

 */
@Component
@Slf4j
public class ConflictDetector {

    private static final int CONFLICT_WINDOW_MINUTES = 30;

    /**
     * Check if two schedules conflict.
     *
     * @param schedule1 the first schedule
     * @param schedule2 the second schedule
     * @return conflict info if detected, empty otherwise
     */
    public Optional<ConflictInfo> checkForConflict(Schedule schedule1, Schedule schedule2) {
        if (!"time".equals(schedule1.getTriggerType())
            || !"time".equals(schedule2.getTriggerType())) {
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

        long minutesDiff = Math.abs(time1.toSecondOfDay() - time2.toSecondOfDay()) / 60;
        if (minutesDiff > CONFLICT_WINDOW_MINUTES) {
            return Optional.empty();
        }

        if (!daysOverlap(config1, config2)) {
            return Optional.empty();
        }

        String target1 = getTarget(schedule1);
        String target2 = getTarget(schedule2);

        if (!targetsOverlap(target1, target2)) {
            return Optional.empty();
        }

        return Optional.of(new ConflictInfo(
            schedule1, schedule2, minutesDiff, target1, target2));
    }

    /**
     * Parse time string in HH:mm or HH:mm:ss format.
     */
    public LocalTime parseTime(String timeStr) {
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

    @SuppressWarnings("unchecked")
    private boolean daysOverlap(Map<String, Object> config1, Map<String, Object> config2) {
        Object days1 = config1.get("weekdays");
        Object days2 = config2.get("weekdays");

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

    /**
     * Determine the type of conflict between two schedules.
     */
    public String determineConflictType(Schedule s1, Schedule s2) {
        String intent1 = getIntent(s1);
        String intent2 = getIntent(s2);

        if (("light.on".equals(intent1) && "light.off".equals(intent2))
            || ("light.off".equals(intent1) && "light.on".equals(intent2))) {
            return "contradiction";
        }

        if (intent1.equals(intent2)) {
            return "duplicate";
        }

        if (intent1.startsWith("scene.") && intent2.startsWith("scene.")) {
            return "scene_overlap";
        }

        if ("light.brightness".equals(intent1) && "light.brightness".equals(intent2)) {
            return "brightness_conflict";
        }

        return "timing_overlap";
    }

    /**
     * Get the intent from a schedule's action.
     */
    public String getIntent(Schedule schedule) {
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

    /**
     * Determine severity based on conflict type and timing.
     */
    public String determineSeverity(Schedule s1, Schedule s2, long minutesDiff) {
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

    /**
     * Container for conflict information.
     */
    public record ConflictInfo(
        Schedule schedule1,
        Schedule schedule2,
        long minutesDiff,
        String target1,
        String target2
    ) {}
}

