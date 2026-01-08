"""
UART Receiver for ESP32 #2 (Main Controller)
Receives sensor data from ESP32 #1 via UART.

UART Protocol (JSON):
{"sensor": "SmartLight-Sensor-1", "temp": 20.5, "hum": 45, "lux": 34, "press": 1013, "audio": 50, "rssi": -75, "ts": 1234567890}
"""

import uasyncio as asyncio
import ujson as json
from machine import UART
import time


class UARTReceiver:
    """Asynchronous UART receiver for sensor data from ESP32 #1."""
    
    def __init__(self, uart_id=2, baudrate=115200, rx_pin=16, tx_pin=17):
        """
        Initialize UART receiver.
        
        Args:
            uart_id: UART port (2 = UART2)
            baudrate: Baud rate (115200 default)
            rx_pin: RX GPIO pin (16 default)
            tx_pin: TX GPIO pin (17 default - unused but required by UART init)
        """
        # Initialize UART with positional arguments for compatibility
        self.uart = UART(uart_id, baudrate, tx=tx_pin, rx=rx_pin)
        self.sensor_data = {}  # {sensor_name: sensor_data}
        self.last_seen = {}    # {sensor_name: timestamp}
        self._buffer = ""      # Line buffer for partial messages
        
        # BLE status from ESP32 #1
        self.ble_status = {
            "connected": 0,
            "ready": 0,
            "expected": 2,
            "sensors": [],
            "last_update": 0,
        }
        
        print("[UART] Initialized on RX={}, Baud={}".format(rx_pin, baudrate))
    
    async def receive_task(self):
        """Continuously receive and parse UART messages."""
        print("[UART] Receiver task started")
        
        while True:
            try:
                # Check if data available
                if self.uart.any():
                    # Read available bytes
                    data = self.uart.read()
                    if data:
                        # Decode and add to buffer (MicroPython doesn't support errors='ignore')
                        try:
                            self._buffer += data.decode('utf-8')
                        except UnicodeError:
                            # Skip invalid UTF-8 sequences
                            pass
                        
                        # Process complete lines (newline-delimited JSON)
                        while '\n' in self._buffer:
                            line, self._buffer = self._buffer.split('\n', 1)
                            line = line.strip()
                            
                            if line:
                                self._parse_message(line)
                
                # Small delay to avoid busy-waiting
                await asyncio.sleep_ms(50)
                
            except Exception as e:
                print("[ERR] UART receive failed:", e)
                await asyncio.sleep(1)
    
    def _parse_message(self, line):
        """Parse a single JSON message from ESP32 #1."""
        try:
            data = json.loads(line)
            
            # Check if this is a status message (type: "status" or "s")
            msg_type = data.get("type")
            if msg_type == "status" or msg_type == "s":
                self._parse_status(data)
                return
            
            # Handle sensor data formats:
            # Compact: {"id": "1", "idx": 0, "t": 22.0, "h": 45.0, "l": 300, "a": 50}
            # Full: {"sensor_id": "sensor_1", "led_index": 0, "temperature": 22.0, ...}
            
            sensor_name = None
            
            # Compact format (preferred)
            if "id" in data:
                sensor_id = data.get("id", "")
                sensor_name = "SmartLight-Sensor-{}".format(sensor_id)
            # sensor_id format
            elif "sensor_id" in data:
                sensor_id = data.get("sensor_id", "")
                if sensor_id.startswith("sensor_"):
                    num = sensor_id.replace("sensor_", "")
                    sensor_name = "SmartLight-Sensor-{}".format(num)
                else:
                    sensor_name = "SmartLight-{}".format(sensor_id)
            # sensor field
            elif "sensor" in data:
                sensor_name = data.get("sensor")
            # led_index fallback
            elif "led_index" in data or "idx" in data:
                led_idx = data.get("led_index", data.get("idx", 0))
                sensor_name = "SmartLight-Sensor-{}".format(led_idx + 1)
            
            if not sensor_name:
                return
            
            # Get existing data or create new (to preserve values not in this update)
            existing = self.sensor_data.get(sensor_name, {})
            
            # Extract sensor values (compact: t/h/l/a, full: temperature/humidity/etc)
            temp = data.get("t", data.get("temperature", data.get("temp", existing.get("temperature"))))
            hum = data.get("h", data.get("humidity", data.get("hum", existing.get("humidity"))))
            lux = data.get("l", data.get("luminosity", data.get("lux", existing.get("luminosity", 0))))
            audio = data.get("a", data.get("audio", existing.get("audio", 0)))
            led_idx = data.get("idx", data.get("led_index", existing.get("led_index", -1)))
            
            # Store sensor data (merge with existing)
            self.sensor_data[sensor_name] = {
                "temperature": temp,
                "humidity": hum,
                "luminosity": lux if lux is not None else 0,
                "audio": audio if audio is not None else 0,
                "audio_peak": audio if audio is not None else 0,
                "name": sensor_name,
                "led_index": led_idx,
                "connected": True,
            }
            
            self.last_seen[sensor_name] = time.time()
            
        except ValueError as e:
            print("[ERR] JSON parse:", line[:40], e)
        except Exception as e:
            print("[ERR] UART parse:", e)
    
    def _parse_status(self, data):
        """Parse BLE status message from ESP32 #1."""
        # Handle both compact and full formats
        connected = data.get("c", data.get("connected", 0))
        ready = data.get("r", data.get("ready", 0))
        expected = data.get("e", data.get("expected", 2))
        
        # Parse sensor list from compact format "1:1,2:1"
        sensors = []
        sensor_list = data.get("list", "")
        if sensor_list:
            for item in sensor_list.split(","):
                if ":" in item:
                    num, is_ready = item.split(":")
                    sensors.append({
                        "name": "SmartLight-Sensor-{}".format(num),
                        "ready": is_ready == "1",
                    })
        else:
            # Full format fallback
            sensors = data.get("sensors", [])
        
        old_ready = self.ble_status.get("ready", -1)
        self.ble_status = {
            "connected": connected,
            "ready": ready,
            "expected": expected,
            "sensors": sensors,
            "last_update": time.time(),
        }
        # Only log when status changes
        if ready != old_ready:
            print("[BLE] {}/{} sensors ready".format(ready, expected))
    
    def get_sensor_data(self, device_name):
        """Get sensor data for a specific device."""
        return self.sensor_data.get(device_name)
    
    def get_all_sensors(self):
        """Get all sensor data with updated connection status."""
        result = {}
        for name, data in self.sensor_data.items():
            # Create a copy with updated connected status
            sensor = data.copy()
            sensor["connected"] = self.is_sensor_alive(name, timeout_s=10)
            result[name] = sensor
        return result
    
    def is_sensor_alive(self, device_name, timeout_s=10):
        """Check if a sensor is still sending data."""
        ts = self.last_seen.get(device_name)
        if ts is None:
            return False
        return (time.time() - ts) < timeout_s
    
    def get_sensor_count(self):
        """Get count of active sensors (seen in last 10 seconds)."""
        now = time.time()
        return sum(1 for _, ts in self.last_seen.items() if (now - ts) < 10)
    
    def cleanup_stale_sensors(self, timeout_s=30):
        """Remove sensors not seen in timeout_s seconds."""
        now = time.time()
        stale = [n for n, ts in self.last_seen.items() if (now - ts) > timeout_s]
        for n in stale:
            self.sensor_data.pop(n, None)
            self.last_seen.pop(n, None)
        return len(stale)
    
    def get_ble_status(self):
        """Get BLE connection status from ESP32 #1 or derive from local data."""
        # Check if status is recent (within 15 seconds)
        if time.time() - self.ble_status.get("last_update", 0) < 15:
            return self.ble_status
        
        # Fall back to counting local sensor data
        active_sensors = []
        for name in self.sensor_data.keys():
            if name.startswith("SmartLight-Sensor"):
                is_alive = self.is_sensor_alive(name, timeout_s=15)
                active_sensors.append({
                    "name": name,
                    "ready": is_alive,
                })
        
        active_count = sum(1 for s in active_sensors if s["ready"])
        return {
            "connected": active_count,
            "ready": active_count,
            "expected": 2,
            "sensors": active_sensors,
        }

