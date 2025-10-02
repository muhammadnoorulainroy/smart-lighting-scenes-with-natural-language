package com.example.smart.lighting.scenes.with_natural.language.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type;
    private UUID deviceId;
    private UUID sceneId;
    private UUID ruleId;
    private Map<String, Object> data;
    private Long timestamp;
}
