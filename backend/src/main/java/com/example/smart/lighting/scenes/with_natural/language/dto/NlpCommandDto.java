package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for NLP command request and response.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlpCommandDto {
    
    /**
     * Request: The natural language input from the user.
     */
    private String text;
    
    /**
     * Response: The parsed command structure.
     */
    private ParsedCommand parsed;
    
    /**
     * Response: Human-readable preview of what will happen.
     */
    private String preview;
    
    /**
     * Response: Whether the command is valid.
     */
    private Boolean valid;
    
    /**
     * Response: Error message if command is invalid.
     */
    private String error;
    
    /**
     * Response: Whether this will create a schedule (vs immediate).
     */
    private Boolean isScheduled;
    
    /**
     * Response: Whether the command was executed.
     */
    private Boolean executed;
    
    /**
     * Response: Execution result message.
     */
    private String result;
    
    /**
     * Response: Timestamp of processing.
     */
    private LocalDateTime timestamp;
    
    /**
     * Response: Conflict analysis for scheduled commands.
     */
    private ConflictAnalysisDto conflictAnalysis;
    
    /**
     * Parsed command structure from LLM.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedCommand {
        /**
         * Intent: light.on, light.off, light.brightness, light.color, scene.apply, scene.create, etc.
         */
        private String intent;
        
        /**
         * Target: room name, "all", or list of rooms.
         */
        private Object target;
        
        /**
         * Parameters: brightness, rgb, color_temp, etc.
         */
        private Map<String, Object> params;
        
        /**
         * Scene name (for scene.apply or scene.create).
         */
        private String scene;
        
        /**
         * Schedule configuration (if command should be scheduled).
         */
        private ScheduleConfig schedule;
        
        /**
         * Confidence score from LLM (0-1).
         */
        private Double confidence;
    }
    
    /**
     * Schedule configuration parsed from natural language.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleConfig {
        /**
         * Time in HH:MM format (for time triggers).
         */
        private String time;
        
        /**
         * Sun event: "sunset" or "sunrise".
         */
        private String trigger;
        
        /**
         * Offset in minutes for sun events.
         */
        private Integer offsetMinutes;
        
        /**
         * Recurrence: "once", "daily", "weekdays", "weekends", or list of days.
         */
        private Object recurrence;
    }
}

