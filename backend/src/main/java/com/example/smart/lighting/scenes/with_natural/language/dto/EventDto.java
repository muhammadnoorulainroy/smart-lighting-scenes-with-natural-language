package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    private LocalDateTime timestamp;
    private String type;
    private UUID actorUserId;
    private String actorUserName;
    private UUID deviceId;
    private String deviceName;
    private UUID ruleId;
    private UUID sceneId;
    private Map<String, Object> detailsJson;
    private Map<String, Object> causeChain;
    
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();
}

