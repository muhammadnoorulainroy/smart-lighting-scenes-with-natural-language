package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for Schedule entities.
 *
 * <p>Contains schedule configuration including trigger type,
 * conditions, and actions to execute.</p>
 *

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDto {
    private UUID id;
    private String name;
    private String description;
    private Boolean enabled;
    private String triggerType;
    private Map<String, Object> triggerConfig;
    private List<Map<String, Object>> conditions;
    private List<Map<String, Object>> actions;
    private LocalDateTime lastTriggeredAt;
    private Integer triggerCount;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
