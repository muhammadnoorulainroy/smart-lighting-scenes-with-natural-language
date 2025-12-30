package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.repository.SceneRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service that executes scheduled lighting automations.
 * Runs every minute to check for schedules that should be triggered.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ScheduleRepository scheduleRepository;
    private final SceneRepository sceneRepository;
    private final MqttService mqttService;

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
        "mon", DayOfWeek.MONDAY,
        "tue", DayOfWeek.TUESDAY,
        "wed", DayOfWeek.WEDNESDAY,
        "thu", DayOfWeek.THURSDAY,
        "fri", DayOfWeek.FRIDAY,
        "sat", DayOfWeek.SATURDAY,
        "sun", DayOfWeek.SUNDAY
    );

    /**
     * Check and execute schedules every minute.
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at second 0
    public void checkSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();

        log.debug("Checking schedules at {}", now);

        List<Schedule> timeSchedules = scheduleRepository.findEnabledTimeSchedules();

        for (Schedule schedule : timeSchedules) {
            if (shouldTrigger(schedule, currentTime, currentDay)) {
                try {
                    executeSchedule(schedule);
                    updateScheduleStats(schedule);
                } catch (Exception e) {
                    log.error("Error executing schedule {}: {}", schedule.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Check if a schedule should be triggered now.
     */
    private boolean shouldTrigger(Schedule schedule, LocalTime currentTime, DayOfWeek currentDay) {
        Map<String, Object> config = schedule.getTriggerConfig();

        // Get scheduled time
        String atTime = (String) config.get("at");
        if (atTime == null) return false;

        LocalTime scheduledTime;
        try {
            // Handle HH:MM or HH:MM:SS format
            if (atTime.length() == 5) {
                scheduledTime = LocalTime.parse(atTime, DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                scheduledTime = LocalTime.parse(atTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        } catch (Exception e) {
            log.warn("Invalid time format in schedule {}: {}", schedule.getId(), atTime);
            return false;
        }

        // Check time (within the same minute)
        if (currentTime.getHour() != scheduledTime.getHour() ||
            currentTime.getMinute() != scheduledTime.getMinute()) {
            return false;
        }

        // Check day of week
        Object weekdaysObj = config.get("weekdays");
        if (weekdaysObj instanceof List<?> weekdays) {
            @SuppressWarnings("unchecked")
            List<String> days = (List<String>) weekdays;
            boolean dayMatches = days.stream()
                .map(String::toLowerCase)
                .map(DAY_MAP::get)
                .filter(Objects::nonNull)
                .anyMatch(d -> d == currentDay);
            if (!dayMatches) {
                return false;
            }
        }
        // If no weekdays specified, assume every day

        return true;
    }

    /**
     * Execute a schedule's actions.
     */
    private void executeSchedule(Schedule schedule) {
        log.info("Executing schedule: {} ({})", schedule.getName(), schedule.getId());

        List<Map<String, Object>> actions = schedule.getActions();
        if (actions == null || actions.isEmpty()) {
            log.warn("Schedule {} has no actions", schedule.getId());
            return;
        }

        for (Map<String, Object> action : actions) {
            executeAction(action);
        }
    }

    /**
     * Execute a single action.
     */
    private void executeAction(Map<String, Object> action) {
        String type = (String) action.get("type");
        String intent = (String) action.get("intent");

        if ("scene".equals(type)) {
            // Apply a scene
            Object sceneIdObj = action.get("scene_id");
            if (sceneIdObj == null) {
                sceneIdObj = action.get("scene");
            }
            if (sceneIdObj != null) {
                Object targetRoom = action.get("target");
                applyScene(sceneIdObj.toString(), targetRoom);
            }
            return;
        }

        // Light control action
        if (intent == null) {
            intent = "light.on"; // Default
        }

        Object target = action.get("target");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) action.get("params");

        Map<String, Object> command = new HashMap<>();

        // Scheduled actions don't set mode - sensors continue working
        switch (intent) {
            case "light.on" -> command.put("on", true);
            case "light.off" -> command.put("on", false);
            case "light.brightness" -> {
                command.put("on", true);
                if (params != null && params.containsKey("brightness")) {
                    command.put("brightness", params.get("brightness"));
                }
            }
            case "light.color" -> {
                command.put("on", true);
                if (params != null && params.containsKey("rgb")) {
                    command.put("rgb", params.get("rgb"));
                }
            }
            case "light.color_temp" -> {
                command.put("on", true);
                if (params != null && params.containsKey("color_temp")) {
                    command.put("color_temp", params.get("color_temp"));
                }
            }
            default -> log.warn("Unknown intent: {}", intent);
        }

        // Send to LEDs
        List<Integer> ledIndices = getLedIndicesForTarget(target);
        for (int ledIndex : ledIndices) {
            mqttService.publishLedCommand(ledIndex, command);
        }

        log.info("Executed action {} on {} LEDs", intent, ledIndices.size());
    }

    /**
     * Apply a scene by ID or name to a specific target (or all if not specified).
     * @param sceneIdOrName the scene ID or name
     * @param targetRoom the target room (null means use scene's default or "all")
     */
    private void applyScene(String sceneIdOrName, Object targetRoom) {
        Optional<Scene> sceneOpt;

        try {
            UUID sceneId = UUID.fromString(sceneIdOrName);
            sceneOpt = sceneRepository.findById(sceneId);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try by name
            sceneOpt = sceneRepository.findByNameIgnoreCaseAndIsActiveTrue(sceneIdOrName);
        }

        if (sceneOpt.isEmpty()) {
            log.warn("Scene not found: {}", sceneIdOrName);
            return;
        }

        Scene scene = sceneOpt.get();
        Map<String, Object> settings = scene.getSettingsJson();

        Map<String, Object> command = new HashMap<>();
        command.put("on", true);
        // Scenes don't set mode - sensors continue working with scene settings as base

        if (settings.containsKey("brightness")) {
            command.put("brightness", settings.get("brightness"));
        }
        if (settings.containsKey("rgb")) {
            command.put("rgb", settings.get("rgb"));
        }
        if (settings.containsKey("color_temp")) {
            command.put("color_temp", settings.get("color_temp"));
        }

        // Use specified target room, or fall back to scene's default, or "all"
        Object effectiveTarget;
        if (targetRoom != null && !targetRoom.toString().isBlank()) {
            effectiveTarget = targetRoom;
        } else {
            effectiveTarget = settings.getOrDefault("target", "all");
        }

        List<Integer> ledIndices = getLedIndicesForTarget(effectiveTarget);

        for (int ledIndex : ledIndices) {
            mqttService.publishLedCommand(ledIndex, command);
        }

        log.info("Applied scene '{}' to {} (LEDs: {})", scene.getName(), effectiveTarget, ledIndices);
    }

    /**
     * Get LED indices for a target.
     */
    private List<Integer> getLedIndicesForTarget(Object target) {
        if (target == null || "all".equalsIgnoreCase(target.toString())) {
            return List.of(0, 1, 2, 3, 4);
        }

        String room = target.toString().toLowerCase().replace(" ", "_").replace("-", "_");

        return switch (room) {
            case "living_room", "living" -> List.of(0);
            case "bedroom" -> List.of(1);
            case "kitchen" -> List.of(2);
            case "bathroom", "bath" -> List.of(3);
            case "hallway" -> List.of(4);
            default -> List.of(0, 1, 2, 3, 4);
        };
    }

    /**
     * Update schedule statistics after execution.
     */
    private void updateScheduleStats(Schedule schedule) {
        schedule.setLastTriggeredAt(LocalDateTime.now());
        schedule.setTriggerCount(schedule.getTriggerCount() + 1);
        scheduleRepository.save(schedule);
    }
}
