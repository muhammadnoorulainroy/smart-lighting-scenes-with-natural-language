package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.entity.Schedule;
import com.example.smart.lighting.scenes.with_natural.language.repository.ScheduleRepository;
import com.example.smart.lighting.scenes.with_natural.language.service.conflict.ConflictAiEnhancer;
import com.example.smart.lighting.scenes.with_natural.language.service.conflict.ConflictDetector;
import com.example.smart.lighting.scenes.with_natural.language.service.conflict.ConflictResolutionGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for detecting and resolving schedule conflicts.
 *
 * <p>Analyzes schedules for timing conflicts, contradictory actions,
 * and overlapping effects. Uses OpenAI to enhance conflict analysis
 * with smart resolution suggestions.</p>
 *
 * <p>This service delegates to specialized components:</p>
 * <ul>
 *   <li>{@link ConflictDetector} - Detects conflicts between schedules</li>
 *   <li>{@link ConflictResolutionGenerator} - Generates resolution options</li>
 *   <li>{@link ConflictAiEnhancer} - AI-enhanced analysis</li>
 * </ul>
 *
 * <h3>Conflict Types:</h3>
 * <ul>
 *   <li><b>contradiction</b> - ON/OFF commands at same time</li>
 *   <li><b>duplicate</b> - Same action at same time</li>
 *   <li><b>scene_overlap</b> - Multiple scenes at same time</li>
 *   <li><b>brightness_conflict</b> - Different brightness levels</li>
 *   <li><b>timing_overlap</b> - Actions within conflict window</li>
 * </ul>
 *

 * @see Schedule
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleConflictService {

    private final ScheduleRepository scheduleRepository;
    private final ConflictDetector conflictDetector;
    private final ConflictResolutionGenerator resolutionGenerator;
    private final ConflictAiEnhancer aiEnhancer;

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
        String severity,
        List<ConflictResolution> resolutions
    ) {}

    /**
     * Represents a possible resolution for a conflict.
     */
    public record ConflictResolution(
        String id,
        String description,
        String action,
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
     *
     * @param newSchedule the schedule to check for conflicts
     * @return conflict analysis result
     */
    public ConflictAnalysisResult detectConflicts(Schedule newSchedule) {
        List<Schedule> existingSchedules = scheduleRepository.findByEnabledTrue();

        existingSchedules = existingSchedules.stream()
            .filter(s -> !s.getId().equals(newSchedule.getId()))
            .toList();

        if (existingSchedules.isEmpty()) {
            return new ConflictAnalysisResult(
                false, List.of(), "No existing schedules to conflict with.");
        }

        List<ScheduleConflict> conflicts = new ArrayList<>();

        for (Schedule existing : existingSchedules) {
            Optional<ConflictDetector.ConflictInfo> conflictInfo =
                conflictDetector.checkForConflict(newSchedule, existing);
            conflictInfo.ifPresent(info ->
                conflicts.add(resolutionGenerator.buildScheduleConflict(info)));
        }

        if (conflicts.isEmpty()) {
            return new ConflictAnalysisResult(false, List.of(), "No conflicts detected.");
        }

        return aiEnhancer.enhanceWithAI(newSchedule, conflicts);
    }

    /**
     * Apply a conflict resolution.
     *
     * @param scheduleId the schedule ID
     * @param resolutionId the resolution ID
     * @param params resolution parameters
     * @return result message
     */
    public String applyResolution(UUID scheduleId, String resolutionId, Map<String, Object> params) {
        return resolutionGenerator.applyResolution(scheduleId, resolutionId, params);
    }
}
