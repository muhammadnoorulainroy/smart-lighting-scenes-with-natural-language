package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceStateDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.SensorReadingDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.entity.Room;
import com.example.smart.lighting.scenes.with_natural.language.entity.SensorReading;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.RoomRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing smart lighting devices.
 *
 * <p>Provides CRUD operations for devices within rooms. Access is restricted
 * based on user roles:</p>
 * <ul>
 *   <li>OWNER - Full access (create, read, update, delete)</li>
 *   <li>RESIDENT - Read-only access</li>
 * </ul>
 *

 * @see Device
 * @see DeviceDto
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class DevicesController {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final SensorReadingRepository sensorReadingRepository;

    /**
     * Get all devices.
     * All authenticated users can view devices.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeviceDto>> getAllDevices(@RequestParam(required = false) UUID roomId) {
        log.debug("Fetching devices with roomId filter: {}", roomId);

        List<Device> devices;
        if (roomId != null) {
            devices = deviceRepository.findByRoomId(roomId);
            log.info("Retrieved {} devices for room {}", devices.size(), roomId);
        } else {
            devices = deviceRepository.findAll();
            log.info("Retrieved {} total devices", devices.size());
        }

        List<DeviceDto> deviceDtos = devices.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(deviceDtos);
    }

    /**
     * Create a device.
     * OWNER and RESIDENT can create devices.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<DeviceDto> createDevice(@RequestBody DeviceDto deviceDto) {
        log.debug("Creating device: name={}, type={}, roomId={}",
            deviceDto.getName(), deviceDto.getType(), deviceDto.getRoomId());

        Room room = roomRepository.findById(deviceDto.getRoomId())
            .orElseThrow(() -> {
                log.error("Failed to create device: room {} not found", deviceDto.getRoomId());
                return new RuntimeException("Room not found");
            });

        Device device = Device.builder()
            .room(room)
            .type(Device.DeviceType.valueOf(deviceDto.getType()))
            .name(deviceDto.getName())
            .mqttCmdTopic(deviceDto.getMqttCmdTopic())
            .mqttStateTopic(deviceDto.getMqttStateTopic())
            .metaJson(deviceDto.getMetaJson() != null ? deviceDto.getMetaJson() : new java.util.HashMap<>())
            .isActive(deviceDto.getIsActive() != null ? deviceDto.getIsActive() : true)
            .build();

        device = deviceRepository.save(device);
        log.info("Device created: id={}, name={}, room={}", device.getId(), device.getName(), room.getName());
        return ResponseEntity.ok(toDto(device));
    }

    /**
     * Update a device.
     * OWNER and RESIDENT can update devices.
     */
    @PutMapping("/{deviceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable UUID deviceId, @RequestBody DeviceDto deviceDto) {
        log.debug("Updating device: id={}", deviceId);

        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> {
                log.error("Failed to update device: device {} not found", deviceId);
                return new RuntimeException("Device not found");
            });

        if (deviceDto.getRoomId() != null && !deviceDto.getRoomId().equals(device.getRoom().getId())) {
            Room room = roomRepository.findById(deviceDto.getRoomId())
                .orElseThrow(() -> {
                    log.error("Failed to update device: room {} not found", deviceDto.getRoomId());
                    return new RuntimeException("Room not found");
                });
            log.info("Moving device {} from room {} to room {}",
                device.getName(), device.getRoom().getName(), room.getName());
            device.setRoom(room);
        }

        if (deviceDto.getName() != null) {
            device.setName(deviceDto.getName());
        }
        if (deviceDto.getType() != null) {
            device.setType(Device.DeviceType.valueOf(deviceDto.getType()));
        }
        if (deviceDto.getMqttCmdTopic() != null) {
            device.setMqttCmdTopic(deviceDto.getMqttCmdTopic());
        }
        if (deviceDto.getMqttStateTopic() != null) {
            device.setMqttStateTopic(deviceDto.getMqttStateTopic());
        }
        if (deviceDto.getMetaJson() != null) {
            device.setMetaJson(deviceDto.getMetaJson());
        }
        if (deviceDto.getIsActive() != null) {
            if (!deviceDto.getIsActive() && device.getIsActive()) {
                log.warn("Deactivating device: id={}, name={}", deviceId, device.getName());
            }
            device.setIsActive(deviceDto.getIsActive());
        }

        device = deviceRepository.save(device);
        log.info("Device updated: id={}, name={}", device.getId(), device.getName());
        return ResponseEntity.ok(toDto(device));
    }

    /**
     * Delete a device.
     * OWNER and RESIDENT can delete devices.
     */
    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID deviceId) {
        log.debug("Deleting device: id={}", deviceId);

        if (!deviceRepository.existsById(deviceId)) {
            log.warn("Attempted to delete non-existent device: id={}", deviceId);
            return ResponseEntity.notFound().build();
        }

        deviceRepository.deleteById(deviceId);
        log.info("Device deleted: id={}", deviceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the latest sensor readings for a device.
     */
    @GetMapping("/{deviceId}/readings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SensorReadingDto>> getDeviceSensorReadings(@PathVariable UUID deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        List<SensorReading> readings = sensorReadingRepository.findLatestReadingsByDeviceId(deviceId);
        List<SensorReadingDto> dtos = readings.stream()
            .map(r -> SensorReadingDto.builder()
                .id(r.getId())
                .deviceId(deviceId)
                .metric(r.getMetric())
                .value(r.getValue())
                .unit(r.getUnit())
                .timestamp(r.getTimestamp())
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private DeviceDto toDto(Device device) {
        DeviceDto.DeviceDtoBuilder builder = DeviceDto.builder()
            .id(device.getId())
            .type(device.getType().name())
            .name(device.getName())
            .mqttCmdTopic(device.getMqttCmdTopic())
            .mqttStateTopic(device.getMqttStateTopic())
            .metaJson(device.getMetaJson())
            .isActive(device.getIsActive())
            .createdAt(device.getCreatedAt())
            .updatedAt(device.getUpdatedAt());

        // Room can be null for MICROCONTROLLER type
        if (device.getRoom() != null) {
            builder.roomId(device.getRoom().getId())
                .roomName(device.getRoom().getName());
        }

        if (device.getDeviceState() != null) {
            builder.deviceState(DeviceStateDto.builder()
                .isOn(device.getDeviceState().getIsOn())
                .brightnessPct(device.getDeviceState().getBrightnessPct())
                .colorTempMired(device.getDeviceState().getColorTempMired())
                .rgbColor(device.getDeviceState().getRgbColor())
                .lastSeen(device.getDeviceState().getLastSeen())
                .updatedAt(device.getDeviceState().getUpdatedAt())
                .build());
        }

        return builder.build();
    }
}
