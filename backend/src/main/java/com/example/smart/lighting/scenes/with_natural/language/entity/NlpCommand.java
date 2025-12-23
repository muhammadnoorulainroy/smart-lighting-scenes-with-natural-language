package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity storing natural language command history.
 * Used for debugging, learning, and audit purposes.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Entity
@Table(name = "nlp_commands", schema = "smartlighting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlpCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Original user input text.
     */
    @Column(name = "raw_input", nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    /**
     * Parsed result from LLM in JSON format.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> parsedJson = new HashMap<>();

    /**
     * Whether the command was executed.
     */
    @Column
    @Builder.Default
    private Boolean executed = false;

    /**
     * Result of execution (success message or error).
     */
    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    /**
     * Whether this created a scheduled command (vs immediate).
     */
    @Column(name = "is_scheduled")
    @Builder.Default
    private Boolean isScheduled = false;

    /**
     * Reference to created schedule (if any).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

