package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnTransformer;
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
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "device_type")
    @ColumnTransformer(write = "?::device_type")
    private DeviceType type;

    @Column(nullable = false)
    private String name;

    @Column(name = "mqtt_cmd_topic")  // Nullable for sensors that only publish
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
        /** RGB or white light fixture (legacy, use LED). */
        LIGHT,
        /** Single environmental sensor (temperature, humidity, motion, etc.). */
        SENSOR,
        /** Physical wall switch or relay (legacy). */
        SWITCH,
        /** Device with multiple sensors (e.g., Adafruit Feather Sense). */
        MULTI_SENSOR,
        /** RGB LED strip or single LED. */
        LED,
        /** ESP32 or other microcontroller - not mapped to a room. */
        MICROCONTROLLER
    }
}
