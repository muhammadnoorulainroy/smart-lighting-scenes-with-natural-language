package com.example.smart.lighting.scenes.with_natural.language.service;

import com.example.smart.lighting.scenes.with_natural.language.service.mqtt.MqttMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MqttService Tests")
class MqttServiceTest {

    @Mock
    private MessageChannel mqttOutputChannel;

    @Mock
    private MqttMessageHandler messageHandler;

    private ObjectMapper objectMapper;
    private MqttService mqttService;

    @Captor
    private ArgumentCaptor<Message<String>> messageCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mqttService = new MqttService(
                mqttOutputChannel,
                objectMapper,
                messageHandler
        );
    }

    @Nested
    @DisplayName("publish()")
    class PublishMethod {

        @Test
        @DisplayName("should publish message with correct topic and payload")
        void shouldPublishMessageWithCorrectTopicAndPayload() {
            String topic = "smart-lighting/command/esp32/led/0";
            Map<String, Object> payload = Map.of(
                    "action", "set_color",
                    "r", 255,
                    "g", 128,
                    "b", 64
            );

            when(mqttOutputChannel.send(any(Message.class), anyLong())).thenReturn(true);

            mqttService.publish(topic, payload);

            verify(mqttOutputChannel).send(messageCaptor.capture(), anyLong());

            Message<String> capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getHeaders().get(MqttHeaders.TOPIC)).isEqualTo(topic);
            assertThat(capturedMessage.getPayload()).contains("action");
            assertThat(capturedMessage.getPayload()).contains("set_color");
        }

        @Test
        @DisplayName("should handle send timeout gracefully")
        void shouldHandleSendTimeoutGracefully() {
            String topic = "smart-lighting/command/esp32/led/0";
            Map<String, Object> payload = Map.of("action", "toggle");

            when(mqttOutputChannel.send(any(Message.class), anyLong())).thenReturn(false);

            mqttService.publish(topic, payload);

            verify(mqttOutputChannel).send(any(Message.class), anyLong());
        }

        @Test
        @DisplayName("should serialize complex payload correctly")
        void shouldSerializeComplexPayloadCorrectly() {
            String topic = "smart-lighting/command/esp32/scene";
            Map<String, Object> payload = Map.of(
                    "scene", "evening",
                    "brightness", 75,
                    "transition_ms", 1000
            );

            when(mqttOutputChannel.send(any(Message.class), anyLong())).thenReturn(true);

            mqttService.publish(topic, payload);

            verify(mqttOutputChannel).send(messageCaptor.capture(), anyLong());
            String capturedPayload = messageCaptor.getValue().getPayload();

            assertThat(capturedPayload).contains("scene");
            assertThat(capturedPayload).contains("evening");
            assertThat(capturedPayload).contains("brightness");
        }

        @Test
        @DisplayName("should handle null payload gracefully")
        void shouldHandleNullPayloadGracefully() {
            String topic = "smart-lighting/command/test";

            when(mqttOutputChannel.send(any(Message.class), anyLong())).thenReturn(true);

            mqttService.publish(topic, null);

            verify(mqttOutputChannel).send(messageCaptor.capture(), anyLong());
            assertThat(messageCaptor.getValue().getPayload()).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("LedCommand record")
    class LedCommandTest {

        @Test
        @DisplayName("should create LedCommand with correct values")
        void shouldCreateLedCommandWithCorrectValues() {
            int[] rgb = {255, 128, 64};
            int brightness = 80;
            boolean on = true;

            MqttService.LedCommand command = new MqttService.LedCommand(rgb, brightness, on);

            assertThat(command.rgb()).containsExactly(255, 128, 64);
            assertThat(command.brightness()).isEqualTo(80);
            assertThat(command.on()).isTrue();
        }

        @Test
        @DisplayName("should serialize LedCommand to JSON correctly")
        void shouldSerializeLedCommandToJson() throws Exception {
            int[] rgb = {255, 0, 0};
            MqttService.LedCommand command = new MqttService.LedCommand(rgb, 100, true);

            String json = objectMapper.writeValueAsString(command);

            assertThat(json).contains("\"rgb\":[255,0,0]");
            assertThat(json).contains("\"brightness\":100");
            assertThat(json).contains("\"on\":true");
        }
    }

    @Nested
    @DisplayName("SceneCommand record")
    class SceneCommandTest {

        @Test
        @DisplayName("should create SceneCommand with scene name")
        void shouldCreateSceneCommandWithSceneName() {
            MqttService.SceneCommand command = new MqttService.SceneCommand("evening");

            assertThat(command.sceneName()).isEqualTo("evening");
        }

        @Test
        @DisplayName("should serialize SceneCommand to JSON correctly")
        void shouldSerializeSceneCommandToJson() throws Exception {
            MqttService.SceneCommand command = new MqttService.SceneCommand("morning");

            String json = objectMapper.writeValueAsString(command);

            assertThat(json).contains("\"sceneName\":\"morning\"");
        }
    }

    @Nested
    @DisplayName("GlobalCommand record")
    class GlobalCommandTest {

        @Test
        @DisplayName("should create GlobalCommand for turning on all lights")
        void shouldCreateGlobalCommandForOn() {
            MqttService.GlobalCommand command = new MqttService.GlobalCommand("on", null, "manual");

            assertThat(command.action()).isEqualTo("on");
            assertThat(command.brightness()).isNull();
            assertThat(command.mode()).isEqualTo("manual");
        }

        @Test
        @DisplayName("should create GlobalCommand for brightness adjustment")
        void shouldCreateGlobalCommandForBrightness() {
            MqttService.GlobalCommand command = new MqttService.GlobalCommand("brightness", 75, "auto");

            assertThat(command.action()).isEqualTo("brightness");
            assertThat(command.brightness()).isEqualTo(75);
            assertThat(command.mode()).isEqualTo("auto");
        }
    }
}
