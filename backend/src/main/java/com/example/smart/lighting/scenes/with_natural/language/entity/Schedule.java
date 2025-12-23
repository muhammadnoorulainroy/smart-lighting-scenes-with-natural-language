package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a scheduled automation rule.
 * Schedules define when and what lighting actions should be triggered.
 *
 * @author Smart Lighting Team
 * @version 1.0
 */
@Entity
@Table(name = "schedules", schema = "smartlighting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Type of trigger: 'time', 'sun', 'sensor'
     */
    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    /**
     * Trigger configuration in JSON format.
     * For 'time': {"at": "07:00:00", "weekdays": ["mon","tue","wed","thu","fri"]}
     * For 'sun': {"event": "sunset", "offset_minutes": -30}
     * For 'sensor': {"entity": "luminosity", "above": 800}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> triggerConfig = new HashMap<>();

    /**
     * Optional conditions that must be met for the schedule to execute.
     * Format: [{"type": "time", "after": "06:00", "before": "22:00"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> conditions = new ArrayList<>();

    /**
     * Actions to execute when triggered.
     * Format: [{"type": "scene", "scene_id": "..."}, {"type": "light", "target": "bedroom", "brightness": 50}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> actions = new ArrayList<>();

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    @Builder.Default
    private Integer triggerCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

