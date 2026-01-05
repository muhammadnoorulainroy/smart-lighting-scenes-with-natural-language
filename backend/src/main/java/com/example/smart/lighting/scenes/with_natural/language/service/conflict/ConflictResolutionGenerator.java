package com.example.smart.lighting.scenes.with_natural.language.service.conflict;

import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ConflictResolution;
import com.example.smart.lighting.scenes.with_natural.language.service.ScheduleConflictService.ScheduleConflict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates resolution options for schedule conflicts.
 *

 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConflictResolutionGenerator {

    private final ScheduleRepository scheduleRepository;
    private final ConflictDetector conflictDetector;

    /**
     * Generate basic resolutions for a conflict.
     *
     * @param s1 first schedule
     * @param s2 second schedule
     * @param minutesDiff time difference in minutes
     * @return list of resolution options
     */
    public List<ConflictResolution> generateBasicResolutions(
            Schedule s1, Schedule s2, long minutesDiff) {
        List<ConflictResolution> resolutions = new ArrayList<>();

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

        resolutions.add(new ConflictResolution(
            "prioritize_existing",
            String.format("Keep '%s' and disable the new schedule", s2.getName()),
            "disable",
            Map.of("schedule_id", s1.getId().toString())
        ));

        resolutions.add(new ConflictResolution(
            "replace_existing",
            String.format("Replace '%s' with the new schedule", s2.getName()),
            "delete",
            Map.of("schedule_id", s2.getId().toString())
        ));

        return resolutions;
    }

    /**
     * Generate a conflict description.
     */
    public String generateConflictDescription(
            Schedule s1, Schedule s2, long minutesDiff, String target1, String target2) {
        String type = conflictDetector.determineConflictType(s1, s2);
        String timeDesc = minutesDiff == 0 ? "at the exact same time"
            : String.format("%d minutes apart", minutesDiff);

        String targetDesc = target1.equals(target2)
            ? String.format("the %s", target1.replace("_", " "))
            : "overlapping areas (including 'all' rooms)";

        return switch (type) {
            case "contradiction" -> String.format(
                "These schedules will turn lights ON and OFF %s for %s. "
                    + "This will cause flickering or unexpected behavior.",
                timeDesc, targetDesc);
            case "duplicate" -> String.format(
                "Both schedules perform the same action %s for %s. One may be redundant.",
                timeDesc, targetDesc);
            case "scene_overlap" -> String.format(
                "Two different scenes will be applied %s to %s. "
                    + "The second scene will immediately override the first.",
                timeDesc, targetDesc);
            case "brightness_conflict" -> String.format(
                "Different brightness levels will be set %s for %s. "
                    + "This may cause visible flickering.",
                timeDesc, targetDesc);
            default -> String.format(
                "These schedules trigger %s and affect %s. They may interfere with each other.",
                timeDesc, targetDesc);
        };
    }

    /**
     * Build a ScheduleConflict from detection info.
     */
    public ScheduleConflict buildScheduleConflict(ConflictDetector.ConflictInfo info) {
        Schedule s1 = info.schedule1();
        Schedule s2 = info.schedule2();

        String type = conflictDetector.determineConflictType(s1, s2);
        String severity = conflictDetector.determineSeverity(s1, s2, info.minutesDiff());
        String description = generateConflictDescription(
            s1, s2, info.minutesDiff(), info.target1(), info.target2());
        List<ConflictResolution> resolutions = generateBasicResolutions(s1, s2, info.minutesDiff());

        return new ScheduleConflict(
            s1.getId(),
            s1.getName(),
            s2.getId(),
            s2.getName(),
            type,
            description,
            severity,
            resolutions
        );
    }

    /**
     * Apply a resolution to a schedule.
     *
     * @param conflictScheduleId the schedule ID
     * @param resolutionId the resolution ID
     * @param params resolution parameters
     * @return result message
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
            default -> {
                return "Resolution applied (custom action)";
            }
        }
        return "Resolution could not be applied";
    }

    private String adjustTime(Schedule schedule, int minutesToAdd) {
        Map<String, Object> config = schedule.getTriggerConfig();
        String timeStr = (String) config.get("at");
        LocalTime time = conflictDetector.parseTime(timeStr);
        if (time != null) {
            return time.plusMinutes(minutesToAdd).format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return timeStr;
    }
}
