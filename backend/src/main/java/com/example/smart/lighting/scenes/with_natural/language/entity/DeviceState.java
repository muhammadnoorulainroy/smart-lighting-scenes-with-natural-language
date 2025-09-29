package com.example.smart.lighting.scenes.with_natural.language.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_state_latest", schema = "smartlighting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceState {

    @Id
    private UUID deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "is_on")
    @Builder.Default
    private Boolean isOn = false;

    @Column(name = "brightness_pct")
    private Integer brightnessPct;

    @Column(name = "color_temp_mired")
    private Integer colorTempMired;

    @Column(name = "rgb_color", length = 7)
    private String rgbColor;

    @Column(name = "last_seen")
    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
