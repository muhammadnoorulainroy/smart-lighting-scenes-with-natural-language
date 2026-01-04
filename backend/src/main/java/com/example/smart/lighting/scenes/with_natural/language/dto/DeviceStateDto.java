package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for device state information.
 *
 * <p>Contains the current operational state of a lighting device
 * including power, brightness, and color settings.</p>
 *

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStateDto {
    private Boolean isOn;
    private Integer brightnessPct;
    private Integer colorTempMired;
    private String rgbColor;
    private LocalDateTime lastSeen;
    private LocalDateTime updatedAt;
}
