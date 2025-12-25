package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Device} entity persistence operations.
 *
 * <p>Provides CRUD operations and custom queries for device management,
 * including eager fetching of device state when needed.</p>
 *
 * @author Smart Lighting Team
 * @version 1.0
 * @see Device
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    /**
     * Finds all devices in a specific room.
     * @param roomId the room UUID
     * @return list of devices in the room
     */
    List<Device> findByRoomId(UUID roomId);

    /**
     * Finds all devices of a specific type.
     * @param type the device type
     * @return list of matching devices
     */
    List<Device> findByType(Device.DeviceType type);

    /**
     * Finds a device by ID with its state eagerly loaded.
     * @param id the device UUID
     * @return optional containing device with state
     */
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.deviceState WHERE d.id = :id")
    Optional<Device> findByIdWithState(@Param("id") UUID id);

    /**
     * Finds all devices in a room with states eagerly loaded.
     * @param roomId the room UUID
     * @return list of devices with states
     */
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.deviceState WHERE d.room.id = :roomId")
    List<Device> findByRoomIdWithState(@Param("roomId") UUID roomId);

    /**
     * Finds a device by room and name.
     * @param roomId the room UUID
     * @param name the device name
     * @return optional containing the device
     */
    Optional<Device> findByRoomIdAndName(UUID roomId, String name);

    /**
     * Checks if a device with the given name exists in the room.
     * @param roomId the room UUID
     * @param name the device name
     * @return true if exists
     */
    boolean existsByRoomIdAndName(UUID roomId, String name);

    /**
     * Finds a device by LED index stored in meta_json.
     * @param ledIndex the LED index
     * @return optional containing the device
     */
    @Query(value = """
        SELECT * FROM smartlighting.devices 
        WHERE meta_json->>'led_index' = CAST(:ledIndex AS TEXT)
        LIMIT 1
        """, nativeQuery = true)
    Optional<Device> findByLedIndex(@Param("ledIndex") int ledIndex);

    /**
     * Finds a device by sensor_id stored in meta_json.
     * @param sensorId the sensor ID (e.g., "SmartLight-Sensor-1")
     * @return optional containing the device
     */
    @Query(value = """
        SELECT * FROM smartlighting.devices 
        WHERE meta_json->>'sensor_id' = :sensorId
        LIMIT 1
        """, nativeQuery = true)
    Optional<Device> findBySensorId(@Param("sensorId") String sensorId);

    /**
     * Finds all devices with their state eagerly loaded.
     * @return list of all devices with states
     */
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.deviceState")
    List<Device> findAllWithState();
}
