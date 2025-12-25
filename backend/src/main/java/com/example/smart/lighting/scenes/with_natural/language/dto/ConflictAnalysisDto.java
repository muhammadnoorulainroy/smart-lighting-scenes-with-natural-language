package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for schedule conflict analysis results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictAnalysisDto {

    private boolean hasConflicts;
    private String summary;
    private List<ConflictDto> conflicts;

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
