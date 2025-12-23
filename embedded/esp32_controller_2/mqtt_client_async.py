"""
Async MQTT Client Wrapper for ESP32 Smart Lighting
Wraps umqtt.simple for use with uasyncio.
"""
import uasyncio as asyncio
import json
import time
from umqtt.simple import MQTTClient
from logger import log, log_err

_SRC = "mqtt"


class AsyncMQTTClient:
    """Async wrapper around umqtt.simple.MQTTClient"""
    
    def __init__(self, client_id, broker, port=1883, user=None, password=None, keepalive=60):
        self.client_id = client_id
        self.broker = broker
        self.port = port
        self.user = user
        self.password = password
        self.keepalive = keepalive
        self.connected = False
        self._client = None
        self._callback = None
        self._lwt_topic = None
        self._lwt_msg = None
    
    async def connect(self, lwt_topic=None, lwt_msg=None):
        """Connect to MQTT broker."""
        try:
            self._lwt_topic = lwt_topic
            self._lwt_msg = lwt_msg
            
            # Convert empty strings to None (umqtt.simple doesn't handle empty strings well)
            mqtt_user = self.user if self.user else None
            mqtt_pass = self.password if self.password else None
            
            self._client = MQTTClient(
                self.client_id,
                self.broker,
                port=self.port,
                user=mqtt_user,
                password=mqtt_pass,
                keepalive=self.keepalive
            )
            
            if lwt_topic and lwt_msg:
                self._client.set_last_will(lwt_topic, lwt_msg, retain=True)
            
            if self._callback:
                self._client.set_callback(self._on_message)
            
            self._client.connect(clean_session=True)
            self.connected = True
            log(_SRC, f"Connected to {self.broker}:{self.port}")
            return True
            
        except Exception as e:
            log_err(_SRC, f"Connect failed: {e}")
            self.connected = False
        return False
    
    def _on_message(self, topic, msg):
        """Internal message callback."""
        if self._callback:
            try:
                topic_str = topic.decode() if isinstance(topic, bytes) else topic
                msg_str = msg.decode() if isinstance(msg, bytes) else msg
                self._callback(topic_str, msg_str)
            except Exception as e:
                log_err(_SRC, f"Callback error: {e}")
    
    def set_callback(self, callback):
        """Set message callback."""
        self._callback = callback
        if self._client:
            self._client.set_callback(self._on_message)
    
    async def subscribe(self, topic, qos=0):
        """Subscribe to topic."""
        if not self.connected or not self._client:
            return False
        try:
            self._client.subscribe(topic, qos)
            log(_SRC, f"Subscribed: {topic}")
            return True
        except Exception as e:
            log_err(_SRC, f"Subscribe failed: {e}")
            return False
    
    async def publish(self, topic, msg, retain=False, qos=0):
        """Publish message."""
        if not self.connected or not self._client:
            return False
        try:
            if isinstance(msg, dict):
                msg = json.dumps(msg)
            if isinstance(topic, str):
                topic = topic.encode()
            if isinstance(msg, str):
                msg = msg.encode()
            self._client.publish(topic, msg, retain, qos)
            return True
        except Exception as e:
            log_err(_SRC, f"Publish failed: {e}")
            self.connected = False
            return False
    
    async def loop(self, duration_ms=10):
        """Check for incoming messages (non-blocking)."""
        if not self.connected or not self._client:
            return
        try:
            self._client.check_msg()
        except Exception as e:
            log_err(_SRC, f"Loop error: {e}")
            self.connected = False
    
    async def disconnect(self):
        """Disconnect from broker."""
        if self._client:
            try:
                self._client.disconnect()
            except:
                pass
        self.connected = False
        self._client = None


class SmartLightingMQTT:
    """Smart Lighting protocol wrapper for MQTT."""
    
    def __init__(self, client, base_topic="smartlight"):
        self.client = client
        self.base = base_topic
        self._command_callback = None
    
    def subscribe_commands(self, callback):
        """Subscribe to command topics."""
        self._command_callback = callback
        self.client.set_callback(self._on_command)
        
        topics = [
            f"{self.base}/command/#",
            f"{self.base}/led/#",
            f"{self.base}/room/#",
            f"{self.base}/scene/#",
            f"{self.base}/config/#",  # Config updates from backend
        ]
        
        async def do_subscribe():
            for topic in topics:
                await self.client.subscribe(topic)
        
        asyncio.create_task(do_subscribe())
    
    def _on_command(self, topic, msg):
        """Handle incoming commands."""
        if self._command_callback:
            asyncio.create_task(self._command_callback(topic, msg))
    
    async def publish_online_status(self, online=True):
        """Publish online/offline status."""
        status = "online" if online else "offline"
        await self.client.publish(f"{self.base}/status/online", status, retain=True)
    
    async def request_config(self):
        """Request configuration from backend."""
        log(_SRC, "Requesting config from backend...")
        await self.client.publish(f"{self.base}/config/request", "get")
    
    async def publish_sensor_data(self, sensor_name, data):
        """Publish sensor data."""
        topic = f"{self.base}/sensor/{sensor_name}"
        await self.client.publish(topic, data)
    
    async def publish_led_state(self, led_index, state):
        """Publish LED state."""
        topic = f"{self.base}/led/{led_index}/state"
        await self.client.publish(topic, state, retain=True)
    
    async def publish_system_status(self, status):
        """Publish system status."""
        topic = f"{self.base}/status/system"
        await self.client.publish(topic, status)
    
    async def publish_system_state(self, state):
        """Publish system state (alias for compatibility)."""
        topic = f"{self.base}/system/state"
        await self.client.publish(topic, state, retain=True)
