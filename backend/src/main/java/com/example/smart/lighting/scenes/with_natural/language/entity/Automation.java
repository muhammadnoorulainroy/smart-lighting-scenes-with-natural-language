package com.example.smart.lighting.scenes.with_natural.language.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Automation configuration loaded from YAML files.
 *
 * <p>Provides Home Assistant-style automation support with triggers,
 * conditions, and actions. Automations are defined in YAML and loaded
 * at runtime.</p>
 *
 * <h3>Execution Modes:</h3>
 * <ul>
 *   <li><b>single</b> - Only one instance runs at a time</li>
 *   <li><b>restart</b> - Cancel current and start new</li>
 *   <li><b>queued</b> - Queue new runs until current completes</li>
 *   <li><b>parallel</b> - Allow multiple concurrent runs</li>
 * </ul>
 *

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

    /**
     * Trigger configuration for an automation.
     * Supports state, time, sun, numeric_state, event, and mqtt triggers.
     */
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

    /**
     * Condition that must be met for an automation to execute.
     * Supports state, numeric_state, time, and template conditions.
     */
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

    /**
     * Action to execute when an automation triggers.
     * Typically calls a service with target entities and data.
     */
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
