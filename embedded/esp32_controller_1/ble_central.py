"""
ESP32 #1 - BLE Central (Connection Mode)
MicroPython implementation that connects to nRF52840 sensors.

Features:
- Connects to multiple BLE sensors simultaneously
- Subscribes to notifications for real-time data
- Uses standard Bluetooth SIG UUIDs
- Forwards sensor data to ESP32 #2 via UART
- Power-efficient: event-driven notifications, no polling
"""

import bluetooth
import time
import json
import struct
import gc
from machine import UART, Pin
from micropython import const

# Standard Bluetooth SIG UUIDs
ENV_SENSING_SERVICE_UUID = bluetooth.UUID(0x181A)
TEMPERATURE_UUID = bluetooth.UUID(0x2A6E)
HUMIDITY_UUID = bluetooth.UUID(0x2A6F)
LUMINOSITY_UUID = bluetooth.UUID(0x2A77)
AUDIO_UUID = bluetooth.UUID(0x2A78)

# Client Characteristic Configuration Descriptor (for enabling notifications)
CCCD_UUID = bluetooth.UUID(0x2902)

# BLE IRQ Events
_IRQ_SCAN_RESULT = const(5)
_IRQ_SCAN_DONE = const(6)
_IRQ_PERIPHERAL_CONNECT = const(7)
_IRQ_PERIPHERAL_DISCONNECT = const(8)
_IRQ_GATTC_SERVICE_RESULT = const(9)
_IRQ_GATTC_SERVICE_DONE = const(10)
_IRQ_GATTC_CHARACTERISTIC_RESULT = const(11)
_IRQ_GATTC_CHARACTERISTIC_DONE = const(12)
_IRQ_GATTC_DESCRIPTOR_RESULT = const(13)
_IRQ_GATTC_DESCRIPTOR_DONE = const(14)
_IRQ_GATTC_READ_RESULT = const(15)
_IRQ_GATTC_READ_DONE = const(16)
_IRQ_GATTC_WRITE_DONE = const(17)
_IRQ_GATTC_NOTIFY = const(18)

# Target sensor names
TARGET_SENSORS = {
    "SmartLight-Sensor-1": {"led_index": 0, "room": "Living"},
    "SmartLight-Sensor-2": {"led_index": 1, "room": "Bedroom"},
}

# UART config for communication with ESP32 #2
UART_TX_PIN = 17
UART_BAUDRATE = 115200


class SensorConnection:
    """Manages a single BLE sensor connection."""
    
    # State machine states for notification setup
    STATE_IDLE = 0
    STATE_DISCOVERING_SERVICE = 1
    STATE_DISCOVERING_CHARS = 2
    STATE_ENABLING_NOTIFY = 3
    STATE_READY = 4
    
    def __init__(self, name, addr, addr_type, config):
        self.name = name
        self.addr = addr
        self.addr_type = addr_type
        self.config = config
        self.conn_handle = None
        self.connected = False
        
        # Service and characteristic handles
        self.service_handle = None
        self.service_end_handle = None
        self.char_handles = {}  # name -> (value_handle, cccd_handle)
        
        # Latest sensor data
        self.temperature = None
        self.humidity = None
        self.luminosity = None
        self.audio = None
        
        # State machine for sequential operations
        self.state = self.STATE_IDLE
        self.notify_queue = []  # Queue of characteristics to enable
        self.pending_write = False  # True if waiting for write confirmation
        self.current_char_uuid = None
    
    def to_dict(self):
        """Convert sensor data to dictionary for UART transmission.
        Only include non-None values to keep JSON compact and valid.
        """
        data = {
            "id": self.name.replace("SmartLight-Sensor-", ""),  # "1" or "2"
            "idx": self.config["led_index"],
        }
        # Only add values that are not None
        if self.temperature is not None:
            data["t"] = round(self.temperature, 1)
        if self.humidity is not None:
            data["h"] = round(self.humidity, 1)
        if self.luminosity is not None:
            data["l"] = self.luminosity
        if self.audio is not None:
            data["a"] = self.audio
        return data


class BLECentral:
    """BLE Central that connects to multiple sensors."""
    
    def __init__(self, uart=None):
        self.ble = bluetooth.BLE()
        self.ble.active(True)
        self.ble.irq(self._irq_handler)
        self.uart = uart
        
        # Discovered devices during scan
        self.discovered = {}  # addr -> (name, addr_type)
        
        # Connected sensors
        self.sensors = {}  # conn_handle -> SensorConnection
        self.pending_connections = {}  # addr -> SensorConnection
        
        # State
        self.scanning = False
        self.scan_complete = False
        
        print("[BLE] Central initialized")
    
    def _irq_handler(self, event, data):
        """Handle BLE IRQ events."""
        
        if event == _IRQ_SCAN_RESULT:
            addr_type, addr, adv_type, rssi, adv_data = data
            addr_str = bytes(addr)
            name = self._decode_name(adv_data)
            
            if name and name in TARGET_SENSORS and addr_str not in self.discovered:
                self.discovered[addr_str] = (name, addr_type, rssi)
                print(f"[BLE] Found: {name} RSSI={rssi}")
        
        elif event == _IRQ_SCAN_DONE:
            self.scanning = False
            self.scan_complete = True
            print("[BLE] Scan complete")
        
        elif event == _IRQ_PERIPHERAL_CONNECT:
            conn_handle, addr_type, addr = data
            addr_str = bytes(addr)
            
            if addr_str in self.pending_connections:
                sensor = self.pending_connections.pop(addr_str)
                sensor.conn_handle = conn_handle
                sensor.connected = True
                sensor.state = SensorConnection.STATE_DISCOVERING_SERVICE
                self.sensors[conn_handle] = sensor
                print(f"[BLE] Connected: {sensor.name} (handle={conn_handle})")
                
                # Start service discovery
                gc.collect()
                self._discover_services(conn_handle)
        
        elif event == _IRQ_PERIPHERAL_DISCONNECT:
            conn_handle, addr_type, addr = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors.pop(conn_handle)
                sensor.connected = False
                print(f"[BLE] Disconnected: {sensor.name}")
                
                # Re-add to discovered for reconnection
                self.discovered[sensor.addr] = (sensor.name, sensor.addr_type, -100)
        
        elif event == _IRQ_GATTC_SERVICE_RESULT:
            conn_handle, start_handle, end_handle, uuid = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors[conn_handle]
                if uuid == ENV_SENSING_SERVICE_UUID:
                    sensor.service_handle = start_handle
                    sensor.service_end_handle = end_handle
                    print(f"[BLE] {sensor.name}: Found Environmental Service")
        
        elif event == _IRQ_GATTC_SERVICE_DONE:
            conn_handle, status = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors[conn_handle]
                
                if sensor.service_handle:
                    sensor.state = SensorConnection.STATE_DISCOVERING_CHARS
                    # Discover characteristics
                    self._discover_characteristics(conn_handle)
                else:
                    sensor.state = SensorConnection.STATE_IDLE
                    print(f"[BLE] {sensor.name}: No Environmental Service found!")
        
        elif event == _IRQ_GATTC_CHARACTERISTIC_RESULT:
            conn_handle, def_handle, value_handle, properties, uuid = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors[conn_handle]
                
                # Store characteristic handles
                if uuid == TEMPERATURE_UUID:
                    sensor.char_handles["temperature"] = (value_handle, None)
                    print(f"[BLE] {sensor.name}: Found Temperature char")
                elif uuid == HUMIDITY_UUID:
                    sensor.char_handles["humidity"] = (value_handle, None)
                    print(f"[BLE] {sensor.name}: Found Humidity char")
                elif uuid == LUMINOSITY_UUID:
                    sensor.char_handles["luminosity"] = (value_handle, None)
                    print(f"[BLE] {sensor.name}: Found Luminosity char")
                elif uuid == AUDIO_UUID:
                    sensor.char_handles["audio"] = (value_handle, None)
                    print(f"[BLE] {sensor.name}: Found Audio char")
        
        elif event == _IRQ_GATTC_CHARACTERISTIC_DONE:
            conn_handle, status = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors[conn_handle]
                sensor.state = SensorConnection.STATE_IDLE
                
                # Small delay then start enabling notifications sequentially
                time.sleep_ms(100)
                gc.collect()
                
                # Enable notifications for all characteristics
                self._enable_notifications(conn_handle)
        
        elif event == _IRQ_GATTC_DESCRIPTOR_RESULT:
            conn_handle, dsc_handle, uuid = data
            
            if conn_handle in self.sensors and uuid == CCCD_UUID:
                sensor = self.sensors[conn_handle]
                # Store CCCD handle for current characteristic
                if sensor.current_char_uuid and sensor.current_char_uuid in sensor.char_handles:
                    value_handle, _ = sensor.char_handles[sensor.current_char_uuid]
                    sensor.char_handles[sensor.current_char_uuid] = (value_handle, dsc_handle)
        
        elif event == _IRQ_GATTC_DESCRIPTOR_DONE:
            conn_handle, status = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors[conn_handle]
                sensor.discovering_descriptors = False
        
        elif event == _IRQ_GATTC_WRITE_DONE:
            conn_handle, value_handle, status = data
            
            if conn_handle in self.sensors:
                sensor = self.sensors[conn_handle]
                sensor.pending_write = False
                
                if status == 0:
                    # Write successful - enable next notification in queue
                    self._enable_next_notification(conn_handle)
                else:
                    print(f"[BLE] {sensor.name}: Write failed status={status}")
                    # Still try next one
                    self._enable_next_notification(conn_handle)
        
        elif event == _IRQ_GATTC_NOTIFY:
            conn_handle, value_handle, notify_data = data
            self._handle_notification(conn_handle, value_handle, notify_data)
    
    def _decode_name(self, adv_data):
        """Decode device name from advertisement data."""
        try:
            i = 0
            while i < len(adv_data):
                length = adv_data[i]
                if length == 0:
                    break
                typ = adv_data[i + 1]
                if typ == 0x09 or typ == 0x08:  # Complete or shortened name
                    return bytes(adv_data[i + 2:i + 1 + length]).decode('utf-8')
                i += 1 + length
        except Exception:
            pass
        return None
    
    def _discover_services(self, conn_handle):
        """Start service discovery."""
        if conn_handle in self.sensors:
            sensor = self.sensors[conn_handle]
            sensor.discovering_service = True
            self.ble.gattc_discover_services(conn_handle, ENV_SENSING_SERVICE_UUID)
    
    def _discover_characteristics(self, conn_handle):
        """Start characteristic discovery."""
        if conn_handle in self.sensors:
            sensor = self.sensors[conn_handle]
            sensor.discovering_chars = True
            self.ble.gattc_discover_characteristics(
                conn_handle, 
                sensor.service_handle, 
                sensor.service_end_handle
            )
    
    def _enable_notifications(self, conn_handle):
        """Queue notifications to enable sequentially (industry-standard approach)."""
        if conn_handle not in self.sensors:
            return
        
        sensor = self.sensors[conn_handle]
        
        # Build queue of characteristics to enable (priority order)
        priority_order = ["temperature", "humidity", "luminosity", "audio"]
        sensor.notify_queue = []
        
        for char_name in priority_order:
            if char_name in sensor.char_handles:
                sensor.notify_queue.append(char_name)
        
        sensor.state = SensorConnection.STATE_ENABLING_NOTIFY
        print(f"[BLE] {sensor.name}: Enabling {len(sensor.notify_queue)} notifications...")
        
        # Start enabling first notification
        self._enable_next_notification(conn_handle)
    
    def _enable_next_notification(self, conn_handle):
        """Enable next notification in queue (called after each write completes)."""
        if conn_handle not in self.sensors:
            return
        
        sensor = self.sensors[conn_handle]
        
        # Check if queue is empty
        if not sensor.notify_queue:
            sensor.state = SensorConnection.STATE_READY
            print(f"[BLE] {sensor.name}: All notifications enabled - READY")
            return
        
        # Don't start new write if one is pending
        if sensor.pending_write:
            return
        
        # Get next characteristic from queue
        char_name = sensor.notify_queue.pop(0)
        
        if char_name not in sensor.char_handles:
            # Skip and try next
            self._enable_next_notification(conn_handle)
            return
        
        value_handle, cccd_handle = sensor.char_handles[char_name]
        target_handle = cccd_handle if cccd_handle else value_handle + 1
        
        # Enable notifications (0x0001)
        enable_notify = struct.pack('<H', 0x0001)
        
        try:
            gc.collect()
            sensor.pending_write = True
            # Use mode 1 (write with response) to get confirmation
            self.ble.gattc_write(conn_handle, target_handle, enable_notify, 1)
            print(f"[BLE] {sensor.name}: Enabling {char_name}...")
        except Exception as e:
            print(f"[BLE] {sensor.name}: Failed {char_name}: {e}")
            sensor.pending_write = False
            # Wait and retry this characteristic
            time.sleep_ms(200)
            gc.collect()
            try:
                sensor.pending_write = True
                self.ble.gattc_write(conn_handle, target_handle, enable_notify, 1)
                print(f"[BLE] {sensor.name}: Retry {char_name}...")
            except Exception as e2:
                print(f"[BLE] {sensor.name}: Skip {char_name}: {e2}")
                sensor.pending_write = False
                # Move to next characteristic
                self._enable_next_notification(conn_handle)
    
    def _handle_notification(self, conn_handle, value_handle, data):
        """Handle incoming notification data."""
        if conn_handle not in self.sensors:
            return
        
        sensor = self.sensors[conn_handle]
        
        # Find which characteristic this notification is for
        for char_name, (v_handle, _) in sensor.char_handles.items():
            if v_handle == value_handle:
                self._update_sensor_value(sensor, char_name, data)
                break
    
    def _update_sensor_value(self, sensor, char_name, data):
        """Update sensor value and send via UART."""
        try:
            if char_name == "temperature":
                # sint16, 0.01°C resolution
                raw = struct.unpack('<h', data)[0]
                sensor.temperature = raw / 100.0
                print(f"[DATA] {sensor.name}: T={sensor.temperature:.1f}°C")
            
            elif char_name == "humidity":
                # uint16, 0.01% resolution
                raw = struct.unpack('<H', data)[0]
                sensor.humidity = raw / 100.0
                print(f"[DATA] {sensor.name}: H={sensor.humidity:.1f}%")
            
            elif char_name == "luminosity":
                # uint16, raw lux
                raw = struct.unpack('<H', data)[0]
                sensor.luminosity = raw
                print(f"[DATA] {sensor.name}: L={sensor.luminosity}lux")
            
            elif char_name == "audio":
                # uint16, 0-100
                raw = struct.unpack('<H', data)[0]
                sensor.audio = raw
                print(f"[DATA] {sensor.name}: A={sensor.audio}")
            
            # Send update via UART
            self._send_uart(sensor)
            
        except Exception as e:
            print(f"[BLE] Parse error: {e}")
    
    def _send_uart(self, sensor):
        """Send sensor data to ESP32 #2 via UART."""
        if not self.uart:
            return
        
        try:
            data = sensor.to_dict()
            msg = json.dumps(data) + "\n"
            self.uart.write(msg.encode())
            time.sleep_ms(10)  # Allow UART buffer to flush
        except Exception as e:
            print("[UART] Send err:", e)
    
    def _send_uart_status(self):
        """Send BLE connection status to ESP32 #2 via UART."""
        if not self.uart:
            return
        
        try:
            # Compact status format
            sensors = []
            for sensor in self.sensors.values():
                # Just send sensor number and ready state
                num = sensor.name.replace("SmartLight-Sensor-", "")
                sensors.append("{}:{}".format(num, 1 if sensor.state == SensorConnection.STATE_READY else 0))
            
            status = {
                "type": "s",  # "s" for status
                "c": len(self.sensors),  # connected
                "r": self.get_ready_count(),  # ready
                "e": len(TARGET_SENSORS),  # expected
                "list": ",".join(sensors),  # "1:1,2:1" format
            }
            msg = json.dumps(status) + "\n"
            self.uart.write(msg.encode())
        except Exception as e:
            print("[UART] Status error:", e)
    
    def _send_all_sensor_data(self):
        """Send complete data for all connected sensors."""
        for sensor in self.sensors.values():
            if sensor.state == SensorConnection.STATE_READY:
                self._send_uart(sensor)
                time.sleep_ms(50)  # Small delay between messages
    
    def scan(self, duration_ms=5000):
        """Scan for target sensors."""
        print(f"[BLE] Scanning for {duration_ms}ms...")
        self.discovered.clear()
        self.scanning = True
        self.scan_complete = False
        
        # Active scan to get device names
        self.ble.gap_scan(duration_ms, 30000, 30000, True)
        
        # Wait for scan to complete
        while self.scanning:
            time.sleep_ms(100)
        
        return self.discovered
    
    def connect(self, addr, name, addr_type, config):
        """Connect to a sensor."""
        sensor = SensorConnection(name, addr, addr_type, config)
        self.pending_connections[addr] = sensor
        
        print(f"[BLE] Connecting to {name}...")
        self.ble.gap_connect(addr_type, addr)
        
        return sensor
    
    def get_connected_count(self):
        """Get number of connected sensors."""
        return len(self.sensors)
    
    def get_ready_count(self):
        """Get number of sensors that are fully ready (notifications enabled)."""
        return sum(1 for s in self.sensors.values() 
                   if s.state == SensorConnection.STATE_READY)
    
    def run(self):
        """Main loop - scan, connect, and receive notifications."""
        print("\n" + "=" * 50)
        print("  ESP32 #1 - BLE Central")
        print("  Connecting to SmartLight Sensors")
        print("=" * 50 + "\n")
        
        last_status = 0
        last_full_send = 0  # Will trigger immediate send on first loop
        reconnect_delay = 0
        first_run = True
        
        while True:
            now = time.time()
            
            # Check if we need to connect to any sensors
            connected_names = {s.name for s in self.sensors.values()}
            needed = set(TARGET_SENSORS.keys()) - connected_names
            
            if needed:
                # Add delay before reconnection attempts to avoid rapid cycling
                if reconnect_delay > 0:
                    reconnect_delay -= 1
                    time.sleep(1)
                    continue
                
                print(f"[BLE] Need to connect: {needed}")
                
                # Scan for missing sensors
                discovered = self.scan(5000)
                
                # Connect to found sensors
                connected_any = False
                for addr, (name, addr_type, rssi) in discovered.items():
                    if name in needed and name in TARGET_SENSORS:
                        self.connect(addr, name, addr_type, TARGET_SENSORS[name])
                        # Wait for connection to establish
                        time.sleep(2)
                        connected_any = True
                
                if not connected_any and needed:
                    # No sensors found, wait before next scan
                    print("[BLE] No target sensors found, waiting...")
                    reconnect_delay = 5  # Wait 5 seconds before next scan
            
            # Main loop - just wait for notifications
            # BLE notifications are handled in IRQ
            time.sleep(2)
            
            # Send full sensor data periodically (every 5 seconds) or on first run
            if first_run or (now - last_full_send >= 5):
                last_full_send = now
                first_run = False
                self._send_all_sensor_data()
                self._send_uart_status()
            
            # Print status periodically (every 10 seconds)
            if now - last_status >= 10:
                last_status = now
                gc.collect()  # Periodic garbage collection
                conn = self.get_connected_count()
                ready = self.get_ready_count()
                print(f"[STATUS] Connected: {conn}/{len(TARGET_SENSORS)}, Ready: {ready}, Mem: {gc.mem_free()}")


def main():
    # Initialize UART for communication with ESP32 #2
    uart = UART(2, baudrate=UART_BAUDRATE, tx=UART_TX_PIN, rx=16)
    print(f"[UART] Initialized TX={UART_TX_PIN} @ {UART_BAUDRATE}")
    
    # Create and run BLE central
    central = BLECentral(uart=uart)
    central.run()


if __name__ == "__main__":
    main()

