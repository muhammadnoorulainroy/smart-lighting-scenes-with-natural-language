package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceStateDto;
import com.example.smart.lighting.scenes.with_natural.language.dto.RoomDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.entity.Room;
import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import com.example.smart.lighting.scenes.with_natural.language.repository.RoomRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.UserRepository;
import com.example.smart.lighting.scenes.with_natural.language.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasAnyRole('OWNER', 'RESIDENT')")
public class RoomsController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllRooms() {
        List<Room> rooms = roomRepository.findAllWithDevices();
        List<RoomDto> roomDtos = rooms.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable UUID roomId) {
        Room room = roomRepository.findByIdWithDevices(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        return ResponseEntity.ok(toDto(room));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RoomDto> createRoom(
            @RequestBody RoomDto roomDto,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        User creator = userRepository.findById(currentUser.getUser().getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Room room = Room.builder()
            .name(roomDto.getName())
            .description(roomDto.getDescription())
            .createdBy(creator)
            .build();

        room = roomRepository.save(room);
        return ResponseEntity.ok(toDto(room));
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable UUID roomId, @RequestBody RoomDto roomDto) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        if (roomDto.getName() != null) {
            room.setName(roomDto.getName());
        }
        if (roomDto.getDescription() != null) {
            room.setDescription(roomDto.getDescription());
        }

        room = roomRepository.save(room);
        return ResponseEntity.ok(toDto(room));
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.notFound().build();
        }
        roomRepository.deleteById(roomId);
        return ResponseEntity.noContent().build();
    }

    private RoomDto toDto(Room room) {
        List<DeviceDto> deviceDtos = room.getDevices().stream()
            .map(this::deviceToDto)
            .collect(Collectors.toList());

        RoomDto.RoomDtoBuilder builder = RoomDto.builder()
            .id(room.getId())
            .name(room.getName())
            .description(room.getDescription())
            .devices(deviceDtos)
            .devicesList(deviceDtos)
            .createdAt(room.getCreatedAt())
            .updatedAt(room.getUpdatedAt());

        if (room.getCreatedBy() != null) {
            builder.createdBy(room.getCreatedBy().getId())
                .createdByName(room.getCreatedBy().getName());
        }

        return builder.build();
    }

    private DeviceDto deviceToDto(Device device) {
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
