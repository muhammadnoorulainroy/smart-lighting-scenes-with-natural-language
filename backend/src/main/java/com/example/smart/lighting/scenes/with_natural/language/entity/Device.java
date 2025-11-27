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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a smart lighting device in the system.
 * 
 * <p>A device belongs to a {@link Room} and communicates via MQTT topics.
 * Each device has a type (LIGHT, SENSOR, or SWITCH) and maintains its
 * current state through a linked {@link DeviceState} entity.</p>
 * 
 * <p>Device names must be unique within a room (enforced by database constraint).</p>
 * 
 * @author Smart Lighting Team
 * @version 1.0
 * @see Room
 * @see DeviceState
 */
@Entity
@Table(name = "devices", schema = "smartlighting", uniqueConstraints = @UniqueConstraint(columnNames = { "room_id",
        "name" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Column(nullable = false)
    private String name;

    @Column(name = "mqtt_cmd_topic", nullable = false)
    private String mqttCmdTopic;

    @Column(name = "mqtt_state_topic", nullable = false)
    private String mqttStateTopic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metaJson = new HashMap<>();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToOne(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeviceState deviceState;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enumeration of supported device types.
     */
    public enum DeviceType {
        /** RGB or white light fixture */
        LIGHT,
        /** Environmental sensor (temperature, humidity, motion) */
        SENSOR,
        /** Physical wall switch or relay */
        SWITCH
    }
}
