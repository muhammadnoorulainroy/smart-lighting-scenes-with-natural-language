package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceStateDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.entity.Room;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
public class DevicesController {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;

    @GetMapping
    public ResponseEntity<List<DeviceDto>> getAllDevices(@RequestParam(required = false) UUID roomId) {
        List<Device> devices;
        if (roomId != null) {
            devices = deviceRepository.findByRoomId(roomId);
        } else {
            devices = deviceRepository.findAll();
        }
        
        List<DeviceDto> deviceDtos = devices.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(deviceDtos);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<DeviceDto> createDevice(@RequestBody DeviceDto deviceDto) {
        Room room = roomRepository.findById(deviceDto.getRoomId())
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
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
        return ResponseEntity.ok(toDto(device));
    }

    @PutMapping("/{deviceId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable UUID deviceId, @RequestBody DeviceDto deviceDto) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("Device not found"));
        
        if (deviceDto.getRoomId() != null && !deviceDto.getRoomId().equals(device.getRoom().getId())) {
            Room room = roomRepository.findById(deviceDto.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));
            device.setRoom(room);
        }
        
        if (deviceDto.getName() != null) device.setName(deviceDto.getName());
        if (deviceDto.getType() != null) device.setType(Device.DeviceType.valueOf(deviceDto.getType()));
        if (deviceDto.getMqttCmdTopic() != null) device.setMqttCmdTopic(deviceDto.getMqttCmdTopic());
        if (deviceDto.getMqttStateTopic() != null) device.setMqttStateTopic(deviceDto.getMqttStateTopic());
        if (deviceDto.getMetaJson() != null) device.setMetaJson(deviceDto.getMetaJson());
        if (deviceDto.getIsActive() != null) device.setIsActive(deviceDto.getIsActive());
        
        device = deviceRepository.save(device);
        return ResponseEntity.ok(toDto(device));
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            return ResponseEntity.notFound().build();
        }
        deviceRepository.deleteById(deviceId);
        return ResponseEntity.noContent().build();
    }

    private DeviceDto toDto(Device device) {
        DeviceDto.DeviceDtoBuilder builder = DeviceDto.builder()
            .id(device.getId())
            .roomId(device.getRoom().getId())
            .roomName(device.getRoom().getName())
            .type(device.getType().name())
            .name(device.getName())
            .mqttCmdTopic(device.getMqttCmdTopic())
            .mqttStateTopic(device.getMqttStateTopic())
            .metaJson(device.getMetaJson())
            .isActive(device.getIsActive())
            .createdAt(device.getCreatedAt())
            .updatedAt(device.getUpdatedAt());
        
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

