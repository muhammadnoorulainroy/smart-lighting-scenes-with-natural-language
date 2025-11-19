"""
OLED Display Manager for ESP32
128x64 SH1107 display via I2C
"""

from machine import I2C, Pin
import sh1107
import time

class OLEDDisplay:
    def __init__(self, sda_pin, scl_pin, width=128, height=64, addr=0x3C):
        """Initialize OLED display"""
        self.enabled = False
        
        # Try I2C bus 0 first, then bus 1
        for bus_id in [0, 1]:
            try:
                print(f"Trying I2C bus {bus_id} (SDA={sda_pin}, SCL={scl_pin})...")
                i2c = I2C(bus_id, scl=Pin(scl_pin), sda=Pin(sda_pin), freq=400000)
                
                # Scan for devices
                devices = i2c.scan()
                print(f"  I2C devices found: {[hex(d) for d in devices]}")
                
                if addr not in devices:
                    print(f"  OLED not found at 0x{addr:02X}")
                    continue
                
                # Initialize OLED
                self.oled = sh1107.SH1107_I2C(width, height, i2c, address=addr, rotate=0)
                self.width = width
                self.height = height
                self.enabled = True
                
                # Test display
                self.clear()
                self.show_text("Smart Lighting", 0, 0)
                self.show_text("Initializing...", 0, 20)
                self.update()
                
                print(f"✓ OLED initialized on bus {bus_id} (SH1107)")
                return
                
            except Exception as e:
                print(f"  Bus {bus_id} failed: {e}")
                continue
        
        print("✗ OLED initialization failed on all buses")
        self.enabled = False
    
    def clear(self):
        """Clear display"""
        if self.enabled:
            self.oled.fill(0)
    
    def update(self):
        """Update display"""
        if self.enabled:
            self.oled.show()
    
    def show_text(self, text, x, y):
        """Show text at position"""
        if self.enabled:
            self.oled.text(text, x, y)
    
    def draw_progress_bar(self, x, y, width, height, percent):
        """Draw a progress bar"""
        if not self.enabled:
            return
        
        # Outer rectangle
        self.oled.rect(x, y, width, height, 1)
        
        # Fill bar
        if percent > 0:
            fill_width = int((width - 2) * (percent / 100.0))
            if fill_width > 0:
                self.oled.fill_rect(x + 1, y + 1, fill_width, height - 2, 1)
    
    def show_home_overview(self, room_states, ble_status, mode):
        """
        Show house overview page
        
        Args:
            room_states: List of dicts with 'name', 'brightness', 'has_sensor'
            ble_status: Dict with sensor connection status
            mode: 'auto' or 'manual'
        """
        if not self.enabled:
            return
        
        self.clear()
        
        # Header
        mode_str = "[AUTO]" if mode == 'auto' else "[MAN]"
        self.show_text(f"HOME {mode_str}", 0, 0)
        
        # Draw line
        self.oled.hline(0, 10, self.width, 1)
        
        # Room list (max 5 rows)
        y = 14
        for i, room in enumerate(room_states[:5]):
            name = room['name'][:8]  # Truncate name
            brightness = room['brightness']
            has_sensor = room.get('has_sensor', False)
            
            # Room name
            self.show_text(name, 0, y)
            
            # Sensor indicator
            if has_sensor:
                self.show_text("*", 50, y)
            
            # Brightness bar
            bar_x = 60
            bar_width = 50
            self.draw_progress_bar(bar_x, y, bar_width, 7, brightness)
            
            # Brightness percentage
            self.show_text(f"{brightness}%", bar_x + bar_width + 2, y)
            
            y += 10
        
        # Footer - BLE status
        self.oled.hline(0, 54, self.width, 1)
        connected_count = sum(1 for v in ble_status.values() if v)
        total_count = len(ble_status)
        self.show_text(f"BLE:{connected_count}/{total_count}", 0, 56)
        
        # Uptime
        uptime_sec = time.ticks_ms() // 1000
        uptime_min = uptime_sec // 60
        self.show_text(f"Up:{uptime_min}m", 70, 56)
        
        self.update()
    
    def show_room_detail(self, room_name, led_index, rgb, brightness, sensor_data):
        """
        Show detailed room view with sensors
        
        Args:
            room_name: Room name
            led_index: LED index
            rgb: (r, g, b) tuple
            brightness: 0-100
            sensor_data: Dict with luminosity, temperature, humidity, audio_peak, connected
        """
        if not self.enabled:
            return
        
        self.clear()
        
        # Header
        sensor_str = "[SENSOR]" if sensor_data['connected'] else "[NO SENSOR]"
        self.show_text(f"{room_name.upper()}", 0, 0)
        self.show_text(sensor_str, 0, 10)
        
        # Brightness bar
        self.show_text("Bright:", 0, 22)
        self.draw_progress_bar(40, 22, 70, 7, brightness)
        self.show_text(f"{brightness}%", 112, 22)
        
        # Sensor data (if connected)
        if sensor_data['connected']:
            y = 32
            self.show_text(f"Lux: {sensor_data['luminosity']}", 0, y)
            y += 10
            self.show_text(f"Temp: {sensor_data['temperature']:.1f}C", 0, y)
            y += 10
            self.show_text(f"Humid: {sensor_data['humidity']:.1f}%", 0, y)
            y += 10
            
            # Audio bar
            self.show_text("Audio:", 0, y)
            audio_percent = min(int(sensor_data['audio_peak'] / 10.23), 100)
            self.draw_progress_bar(40, y, 70, 7, audio_percent)
        else:
            self.show_text("No sensor data", 0, 35)
        
        # RGB value at bottom
        self.show_text(f"RGB:{rgb[0]},{rgb[1]},{rgb[2]}", 0, 56)
        
        self.update()
    
    def show_system_status(self, ble_status, mode, uptime_sec):
        """Show system status page"""
        if not self.enabled:
            return
        
        self.clear()
        
        self.show_text("SYSTEM STATUS", 0, 0)
        self.oled.hline(0, 10, self.width, 1)
        
        y = 14
        self.show_text(f"Mode: {mode.upper()}", 0, y)
        
        y += 12
        connected = sum(1 for v in ble_status.values() if v)
        total = len(ble_status)
        self.show_text(f"BLE: {connected}/{total}", 0, y)
        
        y += 12
        for key, status in ble_status.items():
            status_str = "OK" if status else "LOST"
            self.show_text(f"  {key}: {status_str}", 0, y)
            y += 10
        
        # Uptime
        uptime_min = uptime_sec // 60
        uptime_hr = uptime_min // 60
        uptime_min = uptime_min % 60
        self.show_text(f"Uptime: {uptime_hr}h{uptime_min}m", 0, 56)
        
        self.update()
    
    def show_error(self, message):
        """Show error message"""
        if not self.enabled:
            return
        
        self.clear()
        self.show_text("ERROR", 0, 0)
        self.oled.hline(0, 10, self.width, 1)
        
        # Word wrap message
        words = message.split()
        line = ""
        y = 14
        for word in words:
            test_line = line + " " + word if line else word
            if len(test_line) > 16:  # ~16 chars per line
                self.show_text(line, 0, y)
                line = word
                y += 10
            else:
                line = test_line
        
        if line:
            self.show_text(line, 0, y)
        
        self.update()

