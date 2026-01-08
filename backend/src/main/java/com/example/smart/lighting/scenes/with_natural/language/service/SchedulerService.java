package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.Scene;
import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.repository.SceneRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.example.smart.lighting.scenes.with_natural.language.websocket.WebSocketEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service that executes scheduled lighting automations.
 *
 * <p>Runs every minute to check for time-based schedules that should
 * be triggered. Supports weekday filtering and various lighting actions.</p>
 *
 * <h3>Supported Actions:</h3>
 * <ul>
 *   <li>Apply scenes</li>
 *   <li>Turn lights on/off</li>
 *   <li>Set brightness levels</li>
 *   <li>Set colors</li>
 *   <li>Set color temperature</li>
 * </ul>
 *

 * @see Schedule
 * @see Scene
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ScheduleRepository scheduleRepository;
    private final SceneRepository sceneRepository;
    private final MqttService mqttService;
    private final WebSocketEventService webSocketEventService;
    private final SceneCommandTracker sceneCommandTracker;

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
        "mon", DayOfWeek.MONDAY,
        "tue", DayOfWeek.TUESDAY,
        "wed", DayOfWeek.WEDNESDAY,
        "thu", DayOfWeek.THURSDAY,
        "fri", DayOfWeek.FRIDAY,
        "sat", DayOfWeek.SATURDAY,
        "sun", DayOfWeek.SUNDAY
    );

    /** Track the last processed minute to prevent duplicate executions. */
    private volatile String lastProcessedMinute = "";

    /**
     * Check and execute schedules every minute at second 0.
     */
    @Scheduled(cron = "0 * * * * *", zone = "${app.timezone}")
    public void checkSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();

        // Prevent duplicate execution within the same minute
        String currentMinuteKey = String.format("%02d:%02d", currentTime.getHour(), currentTime.getMinute());
        if (currentMinuteKey.equals(lastProcessedMinute)) {
            log.debug("Skipping duplicate execution for minute {}", currentMinuteKey);
            return;
        }
        lastProcessedMinute = currentMinuteKey;

        log.info("=== Schedule Check at {}:{} ({}) ===",
            String.format("%02d", currentTime.getHour()),
            String.format("%02d", currentTime.getMinute()),
            currentDay);

        List<Schedule> timeSchedules = scheduleRepository.findEnabledTimeSchedules();
        log.info("Found {} enabled time schedules", timeSchedules.size());

        for (Schedule schedule : timeSchedules) {
            Map<String, Object> config = schedule.getTriggerConfig();
            String scheduleTime = (String) config.getOrDefault("at", config.get("time"));
            log.info("Checking schedule '{}': scheduled for {}, weekdays: {}",
                schedule.getName(), scheduleTime, config.get("weekdays"));

            if (shouldTrigger(schedule, currentTime, currentDay)) {
                try {
                    log.info(">>> TRIGGERING schedule: {} <<<", schedule.getName());
                    executeSchedule(schedule);
                    updateScheduleStats(schedule);
                } catch (Exception e) {
                    log.error("Error executing schedule {}: {}", schedule.getId(), e.getMessage(), e);
                }
            } else {
                log.debug("Schedule '{}' not triggered (time mismatch or wrong day)", schedule.getName());
            }
        }
    }

    /**
     * Check if a schedule should be triggered now.
     */
    private boolean shouldTrigger(Schedule schedule, LocalTime currentTime, DayOfWeek currentDay) {
        Map<String, Object> config = schedule.getTriggerConfig();

        // Get scheduled time - support both "at" and "time" keys
        String atTime = (String) config.get("at");
        if (atTime == null) {
            atTime = (String) config.get("time");
        }
        if (atTime == null) {
            return false;
        }

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

        // Check day of week - support both "weekdays" and "days" keys
        Object weekdaysObj = config.get("weekdays");
        if (weekdaysObj == null) {
            weekdaysObj = config.get("days");
        }

        if (weekdaysObj instanceof List<?> weekdays && !((List<?>) weekdays).isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> days = (List<String>) weekdays;
            boolean dayMatches = days.stream()
                .map(day -> {
                    // Support both formats: "MONDAY" and "mon"
                    String dayStr = day.toString().toLowerCase();
                    if (dayStr.length() > 3) {
                        dayStr = dayStr.substring(0, 3); // "monday" -> "mon"
                    }
                    return dayStr;
                })
                .map(DAY_MAP::get)
                .filter(Objects::nonNull)
                .anyMatch(d -> d == currentDay);
            if (!dayMatches) {
                return false;
            }
        }
        // If no weekdays specified or empty list, assume every day

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

        // Send to LEDs with tracking
        List<Integer> ledIndices = getLedIndicesForTarget(target);
        String commandName = "Schedule: " + intent;
        String correlationId = sceneCommandTracker.registerCommand(null, commandName, ledIndices.size());

        for (int ledIndex : ledIndices) {
            Map<String, Object> trackedCommand = new HashMap<>(command);
            trackedCommand.put("correlationId", correlationId);
            mqttService.publishLedCommand(ledIndex, trackedCommand);
        }

        log.info("Executed action {} on {} LEDs (correlationId={})", intent, ledIndices.size(), correlationId);
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

        // Register for tracking and add correlationId
        String correlationId = sceneCommandTracker.registerCommand(
            scene.getId(), "Schedule: " + scene.getName(), ledIndices.size());

        for (int ledIndex : ledIndices) {
            Map<String, Object> trackedCommand = new HashMap<>(command);
            trackedCommand.put("correlationId", correlationId);
            mqttService.publishLedCommand(ledIndex, trackedCommand);
        }

        log.info("Applied scene '{}' to {} (LEDs: {}, correlationId={})",
            scene.getName(), effectiveTarget, ledIndices, correlationId);
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
            case "kitchen" -> List.of(0);
            case "bedroom" -> List.of(1);
            case "bathroom", "bath" -> List.of(2);
            case "hallway" -> List.of(3);
            case "living_room", "living" -> List.of(4);
            default -> List.of(0, 1, 2, 3, 4);
        };
    }

    /**
     * Update schedule statistics after execution and broadcast event.
     */
    private void updateScheduleStats(Schedule schedule) {
        schedule.setLastTriggeredAt(LocalDateTime.now());
        Integer currentCount = schedule.getTriggerCount();
        int newCount = currentCount != null ? currentCount + 1 : 1;
        schedule.setTriggerCount(newCount);
        scheduleRepository.save(schedule);

        // Broadcast WebSocket event for real-time notifications
        webSocketEventService.broadcastScheduleTriggered(
            schedule.getId(), schedule.getName(), newCount);
    }
}
