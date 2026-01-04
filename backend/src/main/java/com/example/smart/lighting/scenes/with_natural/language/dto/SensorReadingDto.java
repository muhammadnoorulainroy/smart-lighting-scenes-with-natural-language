package com.example.smart.lighting.scenes.with_natural.language.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for sensor reading data.
 *
 * <p>Contains a single sensor measurement with metric, value,
 * unit, and timestamp information.</p>
 *

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingDto {
    private Long id;
    private UUID deviceId;
    private String metric;
    private BigDecimal value;
    private String unit;
    private LocalDateTime timestamp;
}
