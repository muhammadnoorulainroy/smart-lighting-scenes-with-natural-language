package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for Scene entities.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneDto {
    private UUID id;
    private String name;
    private String description;
    private String icon;
    private Map<String, Object> settings;
    private Boolean isPreset;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
