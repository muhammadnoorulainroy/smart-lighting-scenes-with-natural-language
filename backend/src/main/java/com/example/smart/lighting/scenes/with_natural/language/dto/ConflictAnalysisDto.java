package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for schedule conflict analysis results.
 *
 * <p>Contains information about scheduling conflicts detected between
 * automation rules, including severity and resolution options.</p>
 *

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictAnalysisDto {

    private boolean hasConflicts;
    private String summary;
    private List<ConflictDto> conflicts;

    /**
     * Details of a single scheduling conflict.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictDto {
        private String scheduleId1;
        private String scheduleName1;
        private String scheduleId2;
        private String scheduleName2;
        private String conflictType;
        private String description;
        private String severity;
        private List<ResolutionDto> resolutions;
    }

    /**
     * A suggested resolution for a scheduling conflict.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolutionDto {
        private String id;
        private String description;
        private String action;
        private Map<String, Object> changes;
    }
}
