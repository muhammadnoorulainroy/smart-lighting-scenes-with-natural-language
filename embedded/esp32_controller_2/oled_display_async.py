"""
Async OLED Display - Multi-Page Display System
Uses SH1107 driver for 128x64 OLED (default 8x8 font)

Pages:
- Page 0: Home (Smart Lighting System + BLE + Date/Time)
- Page 1: Room Brightness Overview
- Page 2+: Individual Sensor Details per Room
"""

import uasyncio as asyncio
from machine import Pin, I2C, RTC
import sh1107
import time
import config

_SRC = "oled_display_async.py"

# France timezone offset (UTC+1)
FRANCE_TZ_OFFSET_SECONDS = 3600  # +1 hour

class AsyncOLEDDisplay:
    """Async OLED display controller with multi-page support"""

    def __init__(self, sda_pin, scl_pin, width, height, addr):
        self.sda_pin = sda_pin
        self.scl_pin = scl_pin
        self.width = width
        self.height = height
        self.addr = addr
        self.display = None
        self.enabled = False
        self.rtc = RTC()  # For date/time display

    async def init_async(self):
        for bus_num in (0, 1):
            try:
                print(f"[{_SRC}] Trying I2C bus {bus_num}...")
                i2c = I2C(bus_num, scl=Pin(self.scl_pin), sda=Pin(self.sda_pin), freq=400000)
                devices = [hex(d) for d in i2c.scan()]
                print(f"[{_SRC}] Devices on bus {bus_num}: {devices}")
                if self.addr in i2c.scan():
                    print(f"[{_SRC}] Found OLED at 0x{self.addr:02x} on bus {bus_num}")
                    try:
                        self.display = sh1107.SH1107_I2C(self.width, self.height, i2c, address=self.addr)
                    except TypeError:
                        self.display = sh1107.SH1107_I2C(self.width, self.height, i2c, address=self.addr)
                    self.display.fill(0)
                    self.display.show()
                    self.enabled = True
                    print(f"[{_SRC}] ✓ SH1107 initialized on bus {bus_num}")
                    return True
            except Exception as e:
                print(f"[{_SRC}] Bus {bus_num} failed: {e}")
                await asyncio.sleep_ms(50)

        print(f"[{_SRC}] OLED not found; continuing without display")
        self.enabled = False
        return False

    async def clear(self):
        """Clear display (for power save mode)"""
        if self.enabled and self.display:
            self.display.fill(0)
            await asyncio.sleep_ms(0)
            self.display.show()
            await asyncio.sleep_ms(0)
    
    def sleep(self):
        """Turn off OLED display (power save)"""
        if self.enabled and self.display:
            self.display.sleep(True)
    
    def wake(self):
        """Turn on OLED display"""
        if self.enabled and self.display:
            self.display.sleep(False)
    
    async def update(self):
        if self.enabled and self.display:
            self.display.show()  # Blocking I2C write (10-20ms)
            await asyncio.sleep_ms(0)  # Yield immediately after I2C write

    def _connected_count(self, ble_status):
        """Count connected BLE sensors from status dict."""
        # ble_status format: {"connected": N, "total": M, "expected": E}
        if isinstance(ble_status, dict):
            # New format with explicit connected count
            if "connected" in ble_status:
                return ble_status.get("connected", 0)
            # Old format - count values that are truthy
            c = 0
            for v in ble_status.values():
                if isinstance(v, dict):
                    if v.get("connected"):
                        c += 1
                elif v:
                    c += 1
            return c
        return 0
    
    def _center_text(self, text, y, color=1):
        """Draw centered text (default 8x8 font)"""
        # 8 pixels per char for default font
        text_width = len(text) * 8
        x = max(0, (self.width - text_width) // 2)
        self.display.text(text, x, y, color)
    
    def _format_time(self):
        """Get current time as HH:MM:SS string (France timezone UTC+1)"""
        try:
            # Use RTC datetime (already set to France time by NTP sync)
            dt = self.rtc.datetime()
            # RTC.datetime() returns: (year, month, day, weekday, hour, minute, second, subsecond)
            return f"{dt[4]:02d}:{dt[5]:02d}:{dt[6]:02d}"
        except:
            # Fallback if RTC not set
            return "00:00:00"
    
    def _format_date(self):
        """Get current date as YYYY-MM-DD string (France timezone)"""
        try:
            dt = self.rtc.datetime()
            return f"{dt[0]:04d}-{dt[1]:02d}-{dt[2]:02d}"
        except:
            return "2025-11-28"
    
    def _format_date_short(self):
        """Get current date as DD/MM string (France format, short for OLED)"""
        try:
            dt = self.rtc.datetime()
            return f"{dt[2]:02d}/{dt[1]:02d}"
        except:
            return "28/11"
    
    async def show_page(self, page, room_states, ble_status, sensor_data, global_mode, uptime_s):
        """
        Show appropriate page based on page number
        
        Args:
            page: Page number (0=home, 1=brightness, 2+=sensor details)
            room_states: List of room state dicts
            ble_status: BLE connection status dict
            sensor_data: Dict of sensor data by sensor_key
            global_mode: "auto" or "manual"
            uptime_s: System uptime in seconds
        """
        if not self.enabled:
            return
        
        if page == 0:
            await self._show_home_page(ble_status, sensor_data)
        elif page == 1:
            await self._show_brightness_page(room_states)
        else:
            # Sensor detail pages (page 2, 3, 4, ...)
            sensor_index = page - config.OLED_PAGE_SENSOR_START
            await self._show_sensor_detail_page(sensor_index, sensor_data, room_states)
    
    async def _show_home_page(self, ble_status, sensor_data):
        """
        Page 0: Home Page
        - "Smart Lighting" centered
        - BLE status (ready/connected/expected)
        - Sensor names that are connected
        - Real-time date and time (France UTC+1)
        """
        self.display.fill(0)
        await asyncio.sleep_ms(0)
        
        # Title (centered, compact)
        self._center_text("Smart", 0)
        self._center_text("Lighting", 10)
        
        # BLE Status from ESP32 #1 - show ready/connected
        ready = ble_status.get("ready", 0)
        connected = ble_status.get("connected", self._connected_count(ble_status))
        expected = ble_status.get("expected", 2)
        
        # Show "Ready: X/Y" format
        status_text = "BLE: {}/{} ready".format(ready, expected)
        self._center_text(status_text, 24)
        
        # Show which sensors are connected from status
        y = 36
        sensors_list = ble_status.get("sensors", [])
        if sensors_list:
            for s in sensors_list[:2]:
                name = s.get("name", "Unknown")
                is_ready = s.get("ready", False)
                short = name.replace("SmartLight-", "")[:10]
                status = "OK" if is_ready else ".."
                self.display.text("{}: {}".format(short, status), 0, y, 1)
                y += 10
        elif sensor_data:
            # Fall back to local sensor data
            for name, data in list(sensor_data.items())[:2]:
                short = name.replace("SmartLight-", "")[:10]
                status = "OK" if data.get("connected", False) else "--"
                self.display.text("{}: {}".format(short, status), 0, y, 1)
                y += 10
        else:
            self.display.text("Waiting...", 0, y, 1)
        
        # Date and Time (France timezone)
        date_str = self._format_date_short()
        time_str = self._format_time()
        datetime_str = f"{date_str} {time_str}"
        self._center_text(datetime_str, 54)
        
        await self.update()
    
    async def _show_brightness_page(self, room_states):
        """
        Page 1: Room Brightness Overview
        Shows brightness level for each room/LED
        """
        self.display.fill(0)
        await asyncio.sleep_ms(0)
        
        # Title
        self.display.text("Room Status", 0, 0, 1)
        
        # Room list (max 5 rooms fit on 64px height with 8x8 font)
        y = 12
        
        for i, room in enumerate(room_states[:5]):
            # Get room name from config if available
            if i < len(config.LED_ROOM_NAMES):
                name = config.LED_ROOM_NAMES[i]
            else:
                name = room.get("room_name", "LED{}".format(i))
            
            brightness = room.get("brightness", 0)
            has_sensor = room.get("has_sensor", False)
            
            # Truncate name to fit (8 chars max)
            name_short = name[:8]
            
            # Format: "Living  *  5%" or "Kitchen    5%"
            sensor_char = "*" if has_sensor else " "
            text = "{:<8}{} {:>2}%".format(name_short, sensor_char, brightness)
            self.display.text(text, 0, y, 1)
            y += 10
        
        await self.update()
    
    async def _show_sensor_detail_page(self, sensor_index, sensor_data, room_states):
        """
        Page 2+: Sensor Detail Pages
        Shows detailed sensor data for one room
        - Temperature
        - Humidity
        - Lux (light level)
        - Audio (noise)
        - Pressure
        """
        self.display.fill(0)
        await asyncio.sleep_ms(0)
        
        # Get sensor key by index from config
        sensor_keys = list(config.SENSOR_DEVICES.keys())
        if sensor_index >= len(sensor_keys):
            self._center_text("No Sensor", 28)
            await self.update()
            return
        
        sensor_key = sensor_keys[sensor_index]
        sensor_config = config.SENSOR_DEVICES[sensor_key]
        room_name = sensor_config.get("room_name", sensor_key)
        
        # Title (max 15 chars for 8x8 font = 120px)
        title = room_name[:15] if len(room_name) > 15 else room_name
        self.display.text(title, 0, 0, 1)
        
        # Try to find sensor data - check both the config key and the full sensor name
        # sensor_key is like "SmartLight-Sensor-1"
        data = sensor_data.get(sensor_key)
        
        # Debug: show what we have
        if not data:
            # Show what sensors we have
            y = 14
            self.display.text("No data for:", 0, y, 1)
            y += 10
            sensor_short = sensor_key[-10:] if len(sensor_key) > 10 else sensor_key
            self.display.text(sensor_short, 0, y, 1)
            y += 14
            # List available sensors
            self.display.text("Available:", 0, y, 1)
            y += 10
            for i, name in enumerate(list(sensor_data.keys())[:3]):
                short = name[-12:] if len(name) > 12 else name
                self.display.text(short, 0, y, 1)
                y += 10
            await self.update()
            return
        
        # Check if connected (recent data)
        if not data.get("connected", False):
            self._center_text("Offline", 28)
            self.display.text("(no recent data)", 0, 44, 1)
            await self.update()
            return
        
        # Display sensor values (compact layout with 8x8 font)
        y = 12
        
        # Temperature
        temp = data.get("temperature", 0)
        if temp is not None:
            self.display.text("T:{:.1f}C".format(temp), 0, y, 1)
        y += 10
        
        # Humidity
        humidity = data.get("humidity", 0)
        if humidity is not None:
            self.display.text("H:{:.1f}%".format(humidity), 0, y, 1)
        y += 10
        
        # Lux (light level)
        lux = data.get("luminosity", 0)
        self.display.text("Lux:{}".format(lux), 0, y, 1)
        y += 10
        
        # Audio (noise level, 0-100)
        audio = data.get("audio", data.get("audio_peak", 0))
        self.display.text("Audio:{}".format(audio), 0, y, 1)
        y += 10
        
        # Pressure (if available)
        pressure = data.get("pressure", 0)
        if pressure and pressure > 0:
            self.display.text("P:{}hPa".format(pressure), 0, y, 1)
        
        await self.update()
    
    # Legacy method for backward compatibility
    async def show_home_overview(self, room_states, ble_status, mode):
        """Legacy method - redirects to home page"""
        await self._show_home_page(ble_status, {})
