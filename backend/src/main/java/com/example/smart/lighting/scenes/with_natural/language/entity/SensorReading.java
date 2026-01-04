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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing a sensor reading from an IoT device.
 *
 * <p>Stores individual sensor measurements including:</p>
 * <ul>
 *   <li>Temperature readings (°C)</li>
 *   <li>Humidity readings (%)</li>
 *   <li>Light/Luminosity readings (lux)</li>
 *   <li>Audio level readings (dB)</li>
 *   <li>Pressure readings (hPa)</li>
 * </ul>
 *
 * <p>Readings are stored with their metric name, value, and unit
 * for time-series analysis and automation triggers.</p>
 *

 * @see Device
 */
@Entity
@Table(name = "sensor_readings", schema = "smartlighting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "metric", nullable = false, length = 50)
    private String metric;

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "unit", length = 20)
    private String unit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "jsonb")
    private Map<String, Object> extraData;
}
