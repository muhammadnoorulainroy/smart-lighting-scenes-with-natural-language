"""
MQTT Handler for ESP32
Handles communication with backend server
"""

import time
import json
from umqtt.simple import MQTTClient

class MQTTHandler:
    def __init__(self, config, led_controller):
        self.config = config
        self.led_controller = led_controller
        self.client = None
        self.connected = False
        
        # MQTT configuration
        self.broker = config.MQTT_BROKER
        self.port = config.MQTT_PORT
        self.client_id = config.MQTT_CLIENT_ID
        self.topic_prefix = config.MQTT_TOPIC_PREFIX
        
        # Callbacks
        self.on_command_callback = None
    
    def connect(self):
        """Connect to MQTT broker"""
        try:
            self.client = MQTTClient(
                self.client_id,
                self.broker,
                port=self.port,
                keepalive=60
            )
            
            # Set callback for incoming messages
            self.client.set_callback(self._on_message)
            
            # Connect
            self.client.connect()
            
            # Subscribe to command topics
            command_topic = f"{self.topic_prefix}/command/{self.client_id}/#"
            self.client.subscribe(command_topic)
            
            self.connected = True
            print(f"✓ Connected to MQTT broker: {self.broker}:{self.port}")
            print(f"  Subscribed to: {command_topic}")
            
            # Publish online status
            self.publish_status("online")
            
        except Exception as e:
            print(f"✗ MQTT connection failed: {e}")
            self.connected = False
    
    def disconnect(self):
        """Disconnect from MQTT broker"""
        if self.client and self.connected:
            try:
                self.publish_status("offline")
                self.client.disconnect()
                self.connected = False
                print("MQTT disconnected")
            except:
                pass
    
    def check_messages(self):
        """Check for new MQTT messages"""
        if self.client and self.connected:
            try:
                self.client.check_msg()
            except Exception as e:
                print(f"MQTT check_msg error: {e}")
                self.connected = False
    
    def _on_message(self, topic, msg):
        """Handle incoming MQTT message"""
        try:
            topic_str = topic.decode('utf-8')
            msg_str = msg.decode('utf-8')
            
            print(f"📨 MQTT: {topic_str}")
            print(f"   Data: {msg_str}")
            
            # Parse topic to determine command type
            parts = topic_str.split('/')
            
            if len(parts) < 4:
                return
            
            # Format: smartlighting/command/esp32-001/<type>
            command_type = parts[3]
            
            # Parse JSON payload
            data = json.loads(msg_str)
            
            # Route to appropriate handler
            if command_type == "global":
                self._handle_global_command(data)
            elif command_type == "led":
                led_index = int(parts[4]) if len(parts) > 4 else 0
                self._handle_led_command(led_index, data)
            elif command_type == "scene":
                self._handle_scene_command(data)
            
        except Exception as e:
            print(f"Error processing MQTT message: {e}")
    
    def _handle_global_command(self, data):
        """Handle global commands (all LEDs)"""
        action = data.get('action')
        
        if action == 'on':
            print("💡 Global ON")
            # Turn on all LEDs
            for i in range(self.config.NUM_LEDS):
                self.led_controller.set_led(i, (255, 255, 255), 50, True)
            self.led_controller.update()
            
        elif action == 'off':
            print("💡 Global OFF")
            # Turn off all LEDs
            self.led_controller.clear()
            
        elif action == 'brightness':
            brightness = data.get('brightness', 50)
            print(f"💡 Global brightness: {brightness}%")
            # Adjust brightness for all LEDs
            for i in range(self.config.NUM_LEDS):
                state = self.led_controller.get_state(i)
                if state:
                    self.led_controller.set_led(i, state['rgb'], brightness, True)
            self.led_controller.update()
        
        # Notify callback
        if self.on_command_callback:
            self.on_command_callback('global', data)
    
    def _handle_led_command(self, led_index, data):
        """Handle individual LED command"""
        rgb = tuple(data.get('rgb', [255, 255, 255]))
        brightness = data.get('brightness', 50)
        on = data.get('on', True)
        
        print(f"💡 LED {led_index}: RGB={rgb}, Bright={brightness}%, On={on}")
        
        self.led_controller.set_led(led_index, rgb, brightness, on)
        self.led_controller.update()
        
        # Notify callback
        if self.on_command_callback:
            self.on_command_callback('led', {'index': led_index, **data})
    
    def _handle_scene_command(self, data):
        """Handle scene activation"""
        scene_name = data.get('sceneName')
        print(f"🎬 Activating scene: {scene_name}")
        
        # TODO: Implement scene logic
        # For now, just apply some predefined scenes
        if scene_name == 'movie':
            # Dim lights, blue tint
            for i in range(self.config.NUM_LEDS):
                self.led_controller.set_led(i, (50, 50, 150), 10, True)
            self.led_controller.update()
        
        elif scene_name == 'relax':
            # Warm colors, medium brightness
            for i in range(self.config.NUM_LEDS):
                self.led_controller.set_led(i, (255, 150, 80), 30, True)
            self.led_controller.update()
        
        elif scene_name == 'bright':
            # Full brightness, white
            for i in range(self.config.NUM_LEDS):
                self.led_controller.set_led(i, (255, 255, 255), 100, True)
            self.led_controller.update()
        
        # Notify callback
        if self.on_command_callback:
            self.on_command_callback('scene', data)
    
    def publish_status(self, status):
        """Publish device status"""
        if not self.connected:
            return
        
        try:
            topic = f"{self.topic_prefix}/status/{self.client_id}"
            payload = json.dumps({
                'status': status,
                'timestamp': time.time(),
                'num_leds': self.config.NUM_LEDS
            })
            self.client.publish(topic, payload)
        except Exception as e:
            print(f"Failed to publish status: {e}")
    
    def publish_sensor_data(self, sensor_id, data):
        """Publish sensor data"""
        if not self.connected:
            return
        
        try:
            topic = f"{self.topic_prefix}/sensor/{sensor_id}"
            payload = json.dumps(data)
            self.client.publish(topic, payload)
        except Exception as e:
            print(f"Failed to publish sensor data: {e}")
    
    def set_command_callback(self, callback):
        """Set callback for command notifications"""
        self.on_command_callback = callback

