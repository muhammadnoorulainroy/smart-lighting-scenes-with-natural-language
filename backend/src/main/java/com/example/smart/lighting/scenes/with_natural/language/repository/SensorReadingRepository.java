package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for sensor reading data access.
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    /**
     * Find all readings for a device ordered by timestamp.
     */
    List<SensorReading> findByDeviceIdOrderByTimestampDesc(UUID deviceId);

    /**
     * Find readings for a device after a certain time.
     */
    List<SensorReading> findByDeviceIdAndTimestampAfterOrderByTimestampDesc(UUID deviceId, LocalDateTime after);

    /**
     * Get the latest reading for each metric for a device.
     */
    @Query(value = """
        SELECT DISTINCT ON (metric) *
        FROM smartlighting.sensor_readings
        WHERE device_id = :deviceId
        ORDER BY metric, timestamp DESC
        """, nativeQuery = true)
    List<SensorReading> findLatestReadingsByDeviceId(@Param("deviceId") UUID deviceId);

    /**
     * Get the latest reading for a specific metric.
     */
    @Query(value = """
        SELECT * FROM smartlighting.sensor_readings
        WHERE device_id = :deviceId AND metric = :metric
        ORDER BY timestamp DESC
        LIMIT 1
        """, nativeQuery = true)
    SensorReading findLatestByDeviceIdAndMetric(@Param("deviceId") UUID deviceId, @Param("metric") String metric);

    /**
     * Delete old readings (for data retention).
     */
    void deleteByTimestampBefore(LocalDateTime before);
}
