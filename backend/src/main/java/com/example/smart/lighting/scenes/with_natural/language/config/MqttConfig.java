package com.example.smart.lighting.scenes.with_natural.language.config;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * MQTT configuration for IoT device communication.
 *
 * <p>Configures MQTT v5 client with Spring Integration for bidirectional
 * communication with ESP32 lighting controllers. Uses Eclipse Paho client
 * with automatic reconnection.</p>
 *
 * <h3>Topic Structure:</h3>
 * <ul>
 *   <li>{@code smartlighting/command/#} - Commands to devices</li>
 *   <li>{@code smartlighting/status/#} - Status updates from devices</li>
 *   <li>{@code smartlighting/sensor/#} - Sensor data (temperature, humidity, etc.)</li>
 *   <li>{@code smartlighting/led/#} - LED state changes</li>
 *   <li>{@code smartlighting/config/#} - Configuration updates</li>
 *   <li>{@code smartlighting/ack/#} - Command acknowledgments</li>
 * </ul>
 *

 * @see org.springframework.integration.mqtt.core.Mqttv5ClientManager
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.topic.command}")
    private String commandTopic;

    @Value("${mqtt.topic.status}")
    private String statusTopic;

    @Value("${mqtt.topic.sensor}")
    private String sensorTopic;

    @Value("${mqtt.topic.led}")
    private String ledTopic;

    @Value("${mqtt.topic.config}")
    private String configTopic;

    @Value("${mqtt.topic.ack}")
    private String ackTopic;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${mqtt.auto-reconnect}")
    private boolean autoReconnect;

    @Value("${mqtt.connection-timeout}")
    private int connectionTimeout;

    @Value("${mqtt.keep-alive-interval}")
    private int keepAliveInterval;

    /**
     * Creates MQTT connection options with configured broker settings.
     *
     * @return configured MqttConnectionOptions for broker connection
     */
    @Bean
    public MqttConnectionOptions mqttConnectionOptions() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(autoReconnect);
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setCleanStart(false);

        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.getBytes());
        }

        log.info("MQTT Connection Options configured for broker: {}", brokerUrl);
        return options;
    }

    /**
     * Creates the MQTT v5 client manager for connection lifecycle management.
     *
     * @return configured Mqttv5ClientManager instance
     */
    @Bean
    public Mqttv5ClientManager mqttClientManager() {
        // Create a simple client manager implementation
        Mqttv5ClientManager clientManager = new Mqttv5ClientManager(mqttConnectionOptions(), clientId);
        log.info("MQTT Client Manager initialized with client ID: {}", clientId);
        return clientManager;
    }

    /**
     * Creates the inbound message channel for receiving MQTT messages.
     *
     * @return DirectChannel for MQTT inbound messages
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * Creates the inbound adapter for receiving messages from subscribed topics.
     *
     * <p>Subscribes to status, sensor, LED state, config, and ack topics.</p>
     *
     * @return configured message-driven channel adapter
     */
    @Bean
    public Mqttv5PahoMessageDrivenChannelAdapter mqttStatusInbound() {
        String[] topics = {statusTopic, sensorTopic, ledTopic, configTopic, ackTopic};
        Mqttv5PahoMessageDrivenChannelAdapter adapter =
            new Mqttv5PahoMessageDrivenChannelAdapter(mqttClientManager(), topics);
        // Using default message converter
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInputChannel());
        log.info("MQTT Inbound Adapter configured for topics: {}", String.join(", ", topics));
        return adapter;
    }

    /**
     * Creates the outbound message channel for sending MQTT messages.
     *
     * @return DirectChannel for MQTT outbound messages
     */
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    /**
     * Creates the outbound message handler for publishing to MQTT topics.
     *
     * <p>Configured as async with default QoS and non-retained messages.</p>
     *
     * @return configured MQTT message handler
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutbound() {
        Mqttv5PahoMessageHandler messageHandler = new Mqttv5PahoMessageHandler(mqttClientManager());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(qos);
        messageHandler.setDefaultRetained(false);
        // Using default message converter
        log.info("MQTT Outbound Handler configured");
        return messageHandler;
    }
}
