package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity for storing system configuration settings.
 * Settings are stored as JSON and can be updated at runtime.
 * ESP32 devices fetch these settings on boot and receive updates via MQTT.
 */
@Entity
@Table(name = "system_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 50)
    private String key;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> settings = new HashMap<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Default configuration categories and their settings.
     */
    public static class Defaults {
        
        public static Map<String, Object> lighting() {
            Map<String, Object> settings = new HashMap<>();
            settings.put("globalMode", "auto");
            settings.put("autoDimEnabled", true);
            settings.put("sensorOverrideEnabled", true);
            settings.put("minBrightness", 0);
            settings.put("maxBrightness", 100);
            settings.put("luxMin", 50);
            settings.put("luxMax", 2000);
            return settings;
        }

        public static Map<String, Object> climate() {
            Map<String, Object> settings = new HashMap<>();
            settings.put("tempMin", 20);
            settings.put("tempMax", 28);
            settings.put("tempBlendStrength", 95);
            settings.put("humidityMin", 30);
            settings.put("humidityMax", 70);
            settings.put("saturationAtMinHumidity", 60);
            settings.put("saturationAtMaxHumidity", 100);
            return settings;
        }

        public static Map<String, Object> audio() {
            Map<String, Object> settings = new HashMap<>();
            settings.put("discoEnabled", true);
            settings.put("audioThreshold", 25);
            settings.put("discoDuration", 3000);
            settings.put("discoSpeed", 100);
            settings.put("flashBrightness", 100);
            return settings;
        }

        public static Map<String, Object> display() {
            Map<String, Object> settings = new HashMap<>();
            settings.put("oledAutoSleep", true);
            settings.put("oledTimeout", 15);
            settings.put("showSensorData", true);
            settings.put("showTime", true);
            return settings;
        }

        public static Map<String, Object> mqtt() {
            Map<String, Object> settings = new HashMap<>();
            settings.put("publishInterval", 2000);
            settings.put("heartbeatInterval", 10000);
            return settings;
        }
    }
}

