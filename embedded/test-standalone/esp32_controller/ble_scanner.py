"""
BLE Scanner/Client for nRF52840 Sensors
Connects to BLE UART services and reads sensor data
"""

import bluetooth
import struct
import time
from micropython import const

# BLE UART Service UUIDs (Nordic UART Service)
_UART_SERVICE_UUID = bluetooth.UUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
_UART_RX_CHAR_UUID = bluetooth.UUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
_UART_TX_CHAR_UUID = bluetooth.UUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

_IRQ_SCAN_RESULT = const(5)
_IRQ_SCAN_DONE = const(6)
_IRQ_PERIPHERAL_CONNECT = const(7)
_IRQ_PERIPHERAL_DISCONNECT = const(8)
_IRQ_GATTC_SERVICE_RESULT = const(9)
_IRQ_GATTC_SERVICE_DONE = const(10)
_IRQ_GATTC_CHARACTERISTIC_RESULT = const(11)
_IRQ_GATTC_CHARACTERISTIC_DONE = const(12)
_IRQ_GATTC_READ_RESULT = const(15)
_IRQ_GATTC_READ_DONE = const(16)
_IRQ_GATTC_WRITE_DONE = const(17)
_IRQ_GATTC_NOTIFY = const(18)

class BLESensor:
    """Manages connection to a single BLE sensor"""
    
    def __init__(self, name, led_index):
        self.name = name
        self.led_index = led_index
        self.addr = None
        self.addr_type = None
        self.conn_handle = None
        self.tx_handle = None
        self.rx_handle = None
        self.connected = False
        
        # Sensor data
        self.luminosity = 0
        self.temperature = 20.0
        self.humidity = 70.0
        self.audio_peak = 0
        self.last_update = 0
        
        # Data buffer
        self.buffer = ""
    
    def parse_sensor_data(self, data_str):
        """Parse sensor data from CSV string: LUX,TEMP,HUMIDITY,AUDIO"""
        try:
            parts = data_str.strip().split(',')
            if len(parts) >= 4:
                self.luminosity = int(float(parts[0]))
                self.temperature = float(parts[1])
                self.humidity = float(parts[2])
                self.audio_peak = int(float(parts[3]))
                self.last_update = time.ticks_ms()
                return True
        except Exception as e:
            print(f"Parse error: {e}")
        return False
    
    def is_data_fresh(self, timeout_ms=2000):
        """Check if sensor data is recent"""
        if self.last_update == 0:
            return False
        return time.ticks_diff(time.ticks_ms(), self.last_update) < timeout_ms


class BLEScanner:
    """BLE Central - scans and connects to sensors"""
    
    def __init__(self, sensor_configs):
        self.ble = bluetooth.BLE()
        self.ble.active(True)
        self.ble.irq(self._irq)
        
        # Sensors to connect to
        self.sensors = {}
        for key, config in sensor_configs.items():
            sensor = BLESensor(config['name'], config['led_index'])
            self.sensors[key] = sensor
            print(f"  Looking for: '{sensor.name}' → LED {sensor.led_index}")
        
        self.scanning = False
        self.scan_callback = None
    
    def _irq(self, event, data):
        """BLE event handler"""
        
        if event == _IRQ_SCAN_RESULT:
            # Scan result
            addr_type, addr, adv_type, rssi, adv_data = data
            name = self._decode_name(adv_data)
            
            # DEBUG: Print all devices found
            if name:
                print(f"BLE Device: {name} | RSSI: {rssi} | Addr: {self._addr_to_str(addr)}")
            
            # Check if this is one of our sensors
            for sensor in self.sensors.values():
                if name and sensor.name in name and not sensor.connected:
                    print(f"✓ MATCH! Found {sensor.name} at {self._addr_to_str(addr)}")
                    sensor.addr = bytes(addr)
                    sensor.addr_type = addr_type
                    # Stop scanning and connect
                    self.ble.gap_scan(None)
                    self.scanning = False
                    self._connect_to_sensor(sensor)
                    break
        
        elif event == _IRQ_SCAN_DONE:
            self.scanning = False
            print("BLE scan complete.")
            if self.scan_callback:
                self.scan_callback()
        
        elif event == _IRQ_PERIPHERAL_CONNECT:
            conn_handle, addr_type, addr = data
            # Find which sensor this is
            for sensor in self.sensors.values():
                if sensor.addr and bytes(addr) == sensor.addr:
                    sensor.conn_handle = conn_handle
                    print(f"Connected to {sensor.name}")
                    # Discover UART service
                    self.ble.gattc_discover_services(conn_handle, _UART_SERVICE_UUID)
                    break
        
        elif event == _IRQ_PERIPHERAL_DISCONNECT:
            conn_handle, _, _ = data
            # Find which sensor disconnected
            for sensor in self.sensors.values():
                if sensor.conn_handle == conn_handle:
                    print(f"{sensor.name} disconnected")
                    sensor.connected = False
                    sensor.conn_handle = None
                    break
        
        elif event == _IRQ_GATTC_SERVICE_RESULT:
            conn_handle, start_handle, end_handle, uuid = data
            # Found UART service, discover characteristics
            if bluetooth.UUID(uuid) == _UART_SERVICE_UUID:
                self.ble.gattc_discover_characteristics(conn_handle, start_handle, end_handle)
        
        elif event == _IRQ_GATTC_CHARACTERISTIC_RESULT:
            conn_handle, def_handle, value_handle, properties, uuid = data
            # Find sensor
            sensor = self._find_sensor_by_conn(conn_handle)
            if sensor:
                if bluetooth.UUID(uuid) == _UART_TX_CHAR_UUID:
                    sensor.tx_handle = value_handle
                    print(f"{sensor.name}: Found TX characteristic")
                elif bluetooth.UUID(uuid) == _UART_RX_CHAR_UUID:
                    sensor.rx_handle = value_handle
                    print(f"{sensor.name}: Found RX characteristic")
        
        elif event == _IRQ_GATTC_CHARACTERISTIC_DONE:
            conn_handle, status = data
            sensor = self._find_sensor_by_conn(conn_handle)
            if sensor and sensor.tx_handle:
                # Subscribe to notifications
                self.ble.gattc_write(conn_handle, sensor.tx_handle + 1, struct.pack('<H', 1), 1)
                sensor.connected = True
                print(f"{sensor.name}: Ready to receive data")
        
        elif event == _IRQ_GATTC_NOTIFY:
            conn_handle, value_handle, notify_data = data
            sensor = self._find_sensor_by_conn(conn_handle)
            if sensor:
                # Accumulate data
                try:
                    # Convert memoryview to bytes, then decode
                    chunk = bytes(notify_data).decode('utf-8')
                    sensor.buffer += chunk
                    
                    # Check for complete line
                    if '\n' in sensor.buffer:
                        lines = sensor.buffer.split('\n')
                        for line in lines[:-1]:
                            if line.strip():
                                sensor.parse_sensor_data(line)
                        sensor.buffer = lines[-1]
                except Exception as e:
                    print(f"Notify error: {e}")
    
    def _find_sensor_by_conn(self, conn_handle):
        """Find sensor by connection handle"""
        for sensor in self.sensors.values():
            if sensor.conn_handle == conn_handle:
                return sensor
        return None
    
    def _connect_to_sensor(self, sensor):
        """Connect to a sensor"""
        if sensor.addr:
            print(f"Connecting to {sensor.name}...")
            self.ble.gap_connect(sensor.addr_type, sensor.addr)
    
    def _decode_name(self, adv_data):
        """Decode device name from advertisement data"""
        try:
            i = 0
            while i < len(adv_data):
                length = adv_data[i]
                if length == 0:
                    break
                typ = adv_data[i + 1]
                # Type 0x09 is complete local name, 0x08 is shortened name
                if typ == 0x09 or typ == 0x08:
                    return bytes(adv_data[i + 2:i + 1 + length]).decode('utf-8')
                i += 1 + length
        except Exception as e:
            print(f"Name decode error: {e}")
        return None
    
    def _addr_to_str(self, addr):
        """Convert address to string"""
        return ':'.join(['%02X' % b for b in addr])
    
    def start_scan(self, duration_ms=5000, callback=None):
        """Start scanning for devices"""
        if not self.scanning:
            print("Scanning for BLE devices...")
            self.scanning = True
            self.scan_callback = callback
            # Active scan (1) to get scan response with device names
            self.ble.gap_scan(duration_ms, 30000, 30000, True)
    
    def stop_scan(self):
        """Stop scanning"""
        if self.scanning:
            self.ble.gap_scan(None)
            self.scanning = False
    
    def get_sensor_data(self, key):
        """Get sensor data for a specific sensor"""
        if key in self.sensors:
            sensor = self.sensors[key]
            if sensor.connected and sensor.is_data_fresh():
                return {
                    'luminosity': sensor.luminosity,
                    'temperature': sensor.temperature,
                    'humidity': sensor.humidity,
                    'audio_peak': sensor.audio_peak,
                    'connected': True
                }
        return {
            'luminosity': 0,
            'temperature': 20.0,
            'humidity': 70.0,
            'audio_peak': 0,
            'connected': False
        }
    
    def get_connection_status(self):
        """Get connection status of all sensors"""
        status = {}
        for key, sensor in self.sensors.items():
            status[key] = sensor.connected
        return status
    
    def reconnect_all(self):
        """Attempt to reconnect disconnected sensors"""
        has_disconnected = False
        for sensor in self.sensors.values():
            if not sensor.connected:
                has_disconnected = True
                break
        
        if has_disconnected and not self.scanning:
            self.start_scan()

