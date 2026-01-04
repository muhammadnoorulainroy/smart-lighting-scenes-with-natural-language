package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
 *
 * <p>Schedules define time-based or event-based triggers for lighting actions.
 * They support various trigger types:</p>
 * <ul>
 *   <li><b>time</b> - Execute at specific times with weekday filtering</li>
 *   <li><b>sun</b> - Execute relative to sunrise/sunset</li>
 *   <li><b>sensor</b> - Execute based on sensor thresholds</li>
 * </ul>
 *
 * <p>Schedules can have conditions and multiple actions, supporting
 * complex automation scenarios.</p>
 *

 * @see User
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
