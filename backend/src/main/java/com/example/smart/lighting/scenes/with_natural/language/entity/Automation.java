package com.example.smart.lighting.scenes.with_natural.language.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Automation configuration (similar to Home Assistant automations)
 * Loaded from YAML file
 */
@Data
public class Automation {
    private String id;
    private String alias;
    private String description;
    private String mode = "single"; // single, restart, queued, parallel
    private List<Trigger> triggers;
    private List<Condition> conditions;
    private List<Action> actions;
    private boolean enabled = true;

    @Data
    public static class Trigger {
        private String trigger; // state, time, sun, numeric_state, event, mqtt
        
        @JsonProperty("entity_id")
        private String entityId;
        
        private String from;
        private String to;
        private String platform;
        private String event; // For sun trigger: sunrise, sunset
        private String offset; // Time offset like "-00:30:00"
        private String at; // For time trigger: "22:00:00"
        private Map<String, Object> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String condition; // state, numeric_state, time, template
        
        @JsonProperty("entity_id")
        private String entityId;
        
        private String state;
        private Integer above;
        private Integer below;
        private String after;
        private String before;
        private List<String> weekday; // List of: mon, tue, wed, thu, fri, sat, sun
        
        @JsonProperty("value_template")
        private String valueTemplate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        private String action; // service call
        private Map<String, Object> target;
        private Map<String, Object> data;
        private String service; // Alternative to action
        
        @JsonProperty("entity_id")
        private String entityId; // Alternative to target
    }
}

