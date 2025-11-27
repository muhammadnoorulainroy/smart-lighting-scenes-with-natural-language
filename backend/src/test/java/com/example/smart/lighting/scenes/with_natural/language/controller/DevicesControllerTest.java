package com.example.smart.lighting.scenes.with_natural.language.controller;

import com.example.smart.lighting.scenes.with_natural.language.dto.DeviceDto;
import com.example.smart.lighting.scenes.with_natural.language.entity.Device;
import com.example.smart.lighting.scenes.with_natural.language.entity.Room;
import com.example.smart.lighting.scenes.with_natural.language.repository.DeviceRepository;
import com.example.smart.lighting.scenes.with_natural.language.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevicesController Tests")
class DevicesControllerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private DevicesController devicesController;

    private Room testRoom;
    private Device testDevice;
    private UUID roomId;
    private UUID deviceId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        deviceId = UUID.randomUUID();

        testRoom = Room.builder()
                .id(roomId)
                .name("Living Room")
                .build();

        testDevice = Device.builder()
                .id(deviceId)
                .room(testRoom)
                .name("Main Light")
                .type(Device.DeviceType.LIGHT)
                .mqttCmdTopic("smart-lighting/command/esp32/led/0")
                .mqttStateTopic("smart-lighting/status/esp32")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("GET /api/devices")
    class GetAllDevices {

        @Test
        @DisplayName("should return all devices when no roomId is provided")
        void shouldReturnAllDevices() {
            List<Device> devices = Arrays.asList(testDevice);
            when(deviceRepository.findAll()).thenReturn(devices);

            ResponseEntity<List<DeviceDto>> response = devicesController.getAllDevices(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getName()).isEqualTo("Main Light");
            verify(deviceRepository).findAll();
        }

        @Test
        @DisplayName("should filter devices by room ID when provided")
        void shouldFilterByRoomId() {
            List<Device> devices = Arrays.asList(testDevice);
            when(deviceRepository.findByRoomId(roomId)).thenReturn(devices);

            ResponseEntity<List<DeviceDto>> response = devicesController.getAllDevices(roomId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            verify(deviceRepository).findByRoomId(roomId);
            verify(deviceRepository, never()).findAll();
        }

        @Test
        @DisplayName("should return empty list when no devices found")
        void shouldReturnEmptyListWhenNoDevices() {
            when(deviceRepository.findAll()).thenReturn(List.of());

            ResponseEntity<List<DeviceDto>> response = devicesController.getAllDevices(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("should correctly map device fields to DTO")
        void shouldCorrectlyMapDeviceToDto() {
            when(deviceRepository.findAll()).thenReturn(List.of(testDevice));

            ResponseEntity<List<DeviceDto>> response = devicesController.getAllDevices(null);

            DeviceDto dto = response.getBody().get(0);
            assertThat(dto.getId()).isEqualTo(deviceId);
            assertThat(dto.getRoomId()).isEqualTo(roomId);
            assertThat(dto.getRoomName()).isEqualTo("Living Room");
            assertThat(dto.getType()).isEqualTo("LIGHT");
            assertThat(dto.getName()).isEqualTo("Main Light");
            assertThat(dto.getMqttCmdTopic()).isEqualTo("smart-lighting/command/esp32/led/0");
            assertThat(dto.getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/devices")
    class CreateDevice {

        @Test
        @DisplayName("should create device successfully")
        void shouldCreateDevice() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            DeviceDto deviceDto = DeviceDto.builder()
                    .roomId(roomId)
                    .name("Main Light")
                    .type("LIGHT")
                    .mqttCmdTopic("smart-lighting/command/esp32/led/0")
                    .mqttStateTopic("smart-lighting/status/esp32")
                    .build();

            ResponseEntity<DeviceDto> response = devicesController.createDevice(deviceDto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getName()).isEqualTo("Main Light");
            verify(deviceRepository).save(any(Device.class));
        }

        @Test
        @DisplayName("should throw exception when room not found")
        void shouldThrowWhenRoomNotFound() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            DeviceDto deviceDto = DeviceDto.builder()
                    .roomId(roomId)
                    .name("Main Light")
                    .type("LIGHT")
                    .build();

            assertThatThrownBy(() -> devicesController.createDevice(deviceDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Room not found");

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("should set default isActive to true when not provided")
        void shouldSetDefaultIsActive() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
                Device device = invocation.getArgument(0);
                device.setId(deviceId);
                device.setRoom(testRoom);
                return device;
            });

            DeviceDto deviceDto = DeviceDto.builder()
                    .roomId(roomId)
                    .name("Test Device")
                    .type("LIGHT")
                    .mqttCmdTopic("topic")
                    .mqttStateTopic("topic")
                    .build();

            ResponseEntity<DeviceDto> response = devicesController.createDevice(deviceDto);

            assertThat(response.getBody().getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/devices/{id}")
    class UpdateDevice {

        @Test
        @DisplayName("should update device name")
        void shouldUpdateDeviceName() {
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            DeviceDto deviceDto = DeviceDto.builder()
                    .name("Updated Light")
                    .build();

            ResponseEntity<DeviceDto> response = devicesController.updateDevice(deviceId, deviceDto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(deviceRepository).save(any(Device.class));
        }

        @Test
        @DisplayName("should throw when device not found")
        void shouldThrowWhenDeviceNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(deviceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            DeviceDto deviceDto = DeviceDto.builder().name("Test").build();

            assertThatThrownBy(() -> devicesController.updateDevice(nonExistentId, deviceDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Device not found");
        }

        @Test
        @DisplayName("should move device to different room")
        void shouldMoveDeviceToDifferentRoom() {
            UUID newRoomId = UUID.randomUUID();
            Room newRoom = Room.builder().id(newRoomId).name("Kitchen").build();

            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
            when(roomRepository.findById(newRoomId)).thenReturn(Optional.of(newRoom));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            DeviceDto deviceDto = DeviceDto.builder().roomId(newRoomId).build();

            ResponseEntity<DeviceDto> response = devicesController.updateDevice(deviceId, deviceDto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(roomRepository).findById(newRoomId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/devices/{id}")
    class DeleteDevice {

        @Test
        @DisplayName("should delete device when found")
        void shouldDeleteDevice() {
            when(deviceRepository.existsById(deviceId)).thenReturn(true);

            ResponseEntity<Void> response = devicesController.deleteDevice(deviceId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(deviceRepository).deleteById(deviceId);
        }

        @Test
        @DisplayName("should return 404 when device not found")
        void shouldReturn404WhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(deviceRepository.existsById(nonExistentId)).thenReturn(false);

            ResponseEntity<Void> response = devicesController.deleteDevice(nonExistentId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(deviceRepository, never()).deleteById(any());
        }
    }
}
