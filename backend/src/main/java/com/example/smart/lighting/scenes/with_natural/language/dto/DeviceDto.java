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
public class DeviceDto {
    private UUID id;
    private UUID roomId;
    private String roomName;
    private String type;
    private String name;
    private String mqttCmdTopic;
    private String mqttStateTopic;
    private Map<String, Object> metaJson;
    private Boolean isActive;
    private DeviceStateDto deviceState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private Map<String, Object> metaJsonMap = new HashMap<>();
}

