package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository for device state data access.
 */
@Repository
public interface DeviceStateRepository extends JpaRepository<DeviceState, UUID> {

    /**
     * Update LED state for a device.
     */
    @Modifying
    @Query("""
        UPDATE DeviceState ds SET
            ds.isOn = :isOn,
            ds.brightnessPct = :brightness,
            ds.rgbColor = :rgbColor,
            ds.lastSeen = :lastSeen
        WHERE ds.deviceId = :deviceId
        """)
    int updateLedState(
        @Param("deviceId") UUID deviceId,
        @Param("isOn") Boolean isOn,
        @Param("brightness") Integer brightness,
        @Param("rgbColor") String rgbColor,
        @Param("lastSeen") LocalDateTime lastSeen
    );

    /**
     * Update last seen timestamp.
     */
    @Modifying
    @Query("UPDATE DeviceState ds SET ds.lastSeen = :lastSeen WHERE ds.deviceId = :deviceId")
    int updateLastSeen(@Param("deviceId") UUID deviceId, @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Upsert device state - insert if not exists, update lastSeen if exists.
     * Uses native query for ON CONFLICT support.
     */
    @Modifying
    @Query(value = """
        INSERT INTO smartlighting.device_state_latest (device_id, is_on, last_seen, updated_at)
        VALUES (:deviceId, true, :lastSeen, :lastSeen)
        ON CONFLICT (device_id)
        DO UPDATE SET last_seen = :lastSeen, updated_at = :lastSeen
        """, nativeQuery = true)
    void upsertLastSeen(@Param("deviceId") UUID deviceId, @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Upsert LED state with all values.
     */
    @Modifying
    @Query(value = """
        INSERT INTO smartlighting.device_state_latest (device_id, is_on, brightness_pct, rgb_color, last_seen, updated_at)
        VALUES (:deviceId, :isOn, :brightness, :rgbColor, :lastSeen, :lastSeen)
        ON CONFLICT (device_id)
        DO UPDATE SET
            is_on = COALESCE(:isOn, device_state_latest.is_on),
            brightness_pct = COALESCE(:brightness, device_state_latest.brightness_pct),
            rgb_color = COALESCE(:rgbColor, device_state_latest.rgb_color),
            last_seen = :lastSeen,
            updated_at = :lastSeen
        """, nativeQuery = true)
    void upsertLedState(
        @Param("deviceId") UUID deviceId,
        @Param("isOn") Boolean isOn,
        @Param("brightness") Integer brightness,
        @Param("rgbColor") String rgbColor,
        @Param("lastSeen") LocalDateTime lastSeen
    );
}
