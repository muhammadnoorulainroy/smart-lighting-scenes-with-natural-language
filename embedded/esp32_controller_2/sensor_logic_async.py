# sensor_logic_async.py
"""
Async Sensor Logic - climate + audio effects.
Restored full logic (matches your original behavior).
"""

import uasyncio as asyncio
import time
from logger import log, log_err

_SRC = "sensor_logic_async.py"

def lerp(a, b, t):
    return a + (b - a) * max(0.0, min(1.0, t))

def clamp(value, min_val, max_val):
    return max(min_val, min(max_val, value))

def rgb_to_hsv(r, g, b):
    r, g, b = r/255.0, g/255.0, b/255.0
    max_c = max(r, g, b)
    min_c = min(r, g, b)
    delta = max_c - min_c

    if delta == 0:
        h = 0
    elif max_c == r:
        h = 60 * (((g - b) / delta) % 6)
    elif max_c == g:
        h = 60 * (((b - r) / delta) + 2)
    else:
        h = 60 * (((r - g) / delta) + 4)

    s = 0 if max_c == 0 else delta / max_c
    v = max_c
    return h, s, v

def hsv_to_rgb(h, s, v):
    c = v * s
    x = c * (1 - abs((h/60) % 2 - 1))
    m = v - c

    if h < 60:
        r, g, b = c, x, 0
    elif h < 120:
        r, g, b = x, c, 0
    elif h < 180:
        r, g, b = 0, c, x
    elif h < 240:
        r, g, b = 0, x, c
    elif h < 300:
        r, g, b = x, 0, c
    else:
        r, g, b = c, 0, x

    return (
        int((r + m) * 255),
        int((g + m) * 255),
        int((b + m) * 255)
    )

def interpolate_gradient(value, min_val, max_val, color_min, color_neutral, color_max):
    t = (value - min_val) / (max_val - min_val)
    t = clamp(t, 0.0, 1.0)

    if t < 0.5:
        lt = t * 2
        return (
            int(lerp(color_min[0], color_neutral[0], lt)),
            int(lerp(color_min[1], color_neutral[1], lt)),
            int(lerp(color_min[2], color_neutral[2], lt)),
        )
    lt = (t - 0.5) * 2
    return (
        int(lerp(color_neutral[0], color_max[0], lt)),
        int(lerp(color_neutral[1], color_max[1], lt)),
        int(lerp(color_neutral[2], color_max[2], lt)),
    )

class AsyncSensorLogic:
    def __init__(self, cfg):
        self.cfg = cfg
        self.sensor_cache = {}
        self.audio_flash_active = {}
        self.audio_flash_start = {}
        self._last_temp_logged = {}
        self._last_humidity_logged = {}
        self._last_brightness_logged = {}  # Track last logged brightness per LED
        self._last_lux_processed = {}  # Track last lux that triggered brightness change (hysteresis)
        self._last_adjusted_brightness = {}  # Track last calculated brightness (for hysteresis)
        log(_SRC, "Sensor logic ready")

    async def update_from_sensors(self, sensor_data):
        """Update sensor cache with new data (merges, doesn't replace)"""
        try:
            self.sensor_cache.update(sensor_data)  # UPDATE, not replace!
        except Exception:
            pass
        await asyncio.sleep_ms(0)

    def adjust_color_for_climate(self, base_rgb, temperature, humidity):
        adjusted_rgb = base_rgb
        
        # Safety check for None values
        if temperature is None:
            temperature = 22.0
        if humidity is None:
            humidity = 50.0

        if self.cfg.TEMP_EFFECTS_ENABLED:
            temp_color = interpolate_gradient(
                temperature,
                self.cfg.TEMP_MIN,
                self.cfg.TEMP_MAX,
                self.cfg.COLOR_GRADIENT_COLD,
                self.cfg.COLOR_GRADIENT_NEUTRAL,
                self.cfg.COLOR_GRADIENT_HOT
            )
            bf = self.cfg.TEMP_BLEND_STRENGTH
            adjusted_rgb = (
                int(lerp(base_rgb[0], temp_color[0], bf)),
                int(lerp(base_rgb[1], temp_color[1], bf)),
                int(lerp(base_rgb[2], temp_color[2], bf)),
            )

        if self.cfg.HUMIDITY_EFFECTS_ENABLED:
            h, s, v = rgb_to_hsv(*adjusted_rgb)

            hum_norm = (humidity - self.cfg.HUMIDITY_MIN) / (self.cfg.HUMIDITY_MAX - self.cfg.HUMIDITY_MIN)
            hum_norm = clamp(hum_norm, 0.0, 1.0)

            target_sat = lerp(self.cfg.SATURATION_AT_MIN_HUMIDITY,
                              self.cfg.SATURATION_AT_MAX_HUMIDITY,
                              hum_norm)
            s = target_sat

            if getattr(self.cfg, "HUE_SHIFT_ENABLED", False):
                hue_shift = (hum_norm - 0.5) * self.cfg.HUE_SHIFT_AMOUNT
                h = (h + hue_shift) % 360

            adjusted_rgb = hsv_to_rgb(h, s, v)

        return adjusted_rgb

    async def calculate_led_state(self, led_index, base_rgb, base_brightness):
        sensor_key = None
        for k, scfg in self.cfg.SENSOR_DEVICES.items():
            if scfg["led_index"] == led_index:
                sensor_key = k
                break

        if not sensor_key:
            # No sensor mapped to this LED
            return base_rgb, base_brightness
            
        if sensor_key not in self.sensor_cache:
            # Sensor mapped but no data yet
            return base_rgb, base_brightness

        data = self.sensor_cache[sensor_key]
        
        # Check if data is None (sensor found but no data received yet)
        if data is None:
            return base_rgb, base_brightness

        # DISCO MODE! 🎉 (3-second rainbow when audio detected)
        if self.audio_flash_active.get(led_index):
            elapsed = time.ticks_diff(time.ticks_ms(), self.audio_flash_start.get(led_index, 0))
            if elapsed < self.cfg.AUDIO_DISCO_DURATION:
                # Rainbow disco effect - cycle through colors
                disco_speed = getattr(self.cfg, "AUDIO_DISCO_SPEED", 100)
                hue = (elapsed // disco_speed * 60) % 360  # Change color every 100ms
                disco_rgb = hsv_to_rgb(hue, 1.0, 1.0)  # Full saturation, full brightness
                return disco_rgb, self.cfg.AUDIO_FLASH_BRIGHTNESS
            self.audio_flash_active[led_index] = False

        audio = data.get("audio", 0)
        if audio > self.cfg.AUDIO_THRESHOLD:
            self.audio_flash_active[led_index] = True
            self.audio_flash_start[led_index] = time.ticks_ms()
            log(_SRC, f"🎉 DISCO MODE! LED {led_index}: audio={audio}")
            # Return immediate disco effect
            disco_rgb = hsv_to_rgb(0, 1.0, 1.0)  # Start with red
            return disco_rgb, self.cfg.AUDIO_FLASH_BRIGHTNESS

        # Get sensor values with safe defaults for None
        temp = data.get("temperature")
        hum = data.get("humidity")
        lux = data.get("luminosity", 0)
        audio = data.get("audio", 0)
        
        # Use defaults if values are None (sensor hasn't reported yet)
        if temp is None:
            temp = 22.0
        if hum is None:
            hum = 50.0
        if lux is None:
            lux = 0

        # Debug: Log sensor values only on significant changes
        last_t = self._last_temp_logged.get(led_index)
        last_h = self._last_humidity_logged.get(led_index)
        if last_t is None or last_h is None or abs(temp - last_t) >= 2.0 or abs(hum - last_h) >= 10.0:
            log(_SRC, f"LED{led_index} Sensors: T={temp:.1f}C H={hum:.0f}% L={lux} A={audio}")

        adjusted_rgb = self.adjust_color_for_climate(base_rgb, temp, hum)
        adjusted_brightness = base_brightness

        # Log climate effect only on significant changes
        if adjusted_rgb != base_rgb:
            last_t = self._last_temp_logged.get(led_index)
            last_h = self._last_humidity_logged.get(led_index)
            if last_t is None or last_h is None or abs(temp - last_t) >= 2.0 or abs(hum - last_h) >= 10.0:
                log(_SRC, f"LED{led_index} 🌡️T={temp:.1f}C H={hum:.0f}% {base_rgb}→{adjusted_rgb}")
                self._last_temp_logged[led_index] = temp
                self._last_humidity_logged[led_index] = hum

        if self.cfg.AUTO_DIM_ENABLED:
            # Lux-based brightness with hysteresis and 5% steps
            # Rule: Only adjust brightness if lux changed by more than 20 units
            # Inverse relationship: high lux → low brightness, low lux → high brightness
            
            last_lux = self._last_lux_processed.get(led_index, None)
            
            # Check if we should update brightness (hysteresis)
            should_update = False
            if last_lux is None:
                should_update = True  # First time
            elif abs(lux - last_lux) > 20:
                should_update = True  # Lux changed by more than 20
            
            if should_update:
                # Map lux to brightness percentage (inverse relationship)
                # Lux 0-100: 100% brightness (dark room)
                # Lux 2000+: 0% brightness (very bright, LED OFF)
                
                # Get brightness limits from config
                min_br = getattr(self.cfg, "MIN_BRIGHTNESS", 0)
                max_br = getattr(self.cfg, "MAX_BRIGHTNESS", 100)
                
                if lux >= self.cfg.LUX_MAX:
                    # Very bright room - use minimum brightness
                    target_brightness_percent = min_br
                elif lux <= self.cfg.LUX_MIN:
                    # Dark room - use maximum brightness
                    target_brightness_percent = max_br
                else:
                    # Interpolate inversely between min and max
                    lux_factor = (lux - self.cfg.LUX_MIN) / (self.cfg.LUX_MAX - self.cfg.LUX_MIN)
                    # Invert: 0 lux → max_br, LUX_MAX → min_br
                    target_brightness_percent = max_br - (lux_factor * (max_br - min_br))
                
                # Quantize to 1% steps (was 5%, now more precise for low brightness)
                target_brightness_percent = round(target_brightness_percent)
                target_brightness_percent = int(clamp(target_brightness_percent, min_br, max_br))
                
                # Use the calculated brightness directly (don't multiply by base!)
                adjusted_brightness = target_brightness_percent
                
                # Store the adjusted brightness (for hysteresis)
                self._last_adjusted_brightness[led_index] = adjusted_brightness
                
                # Log the change
                last_brightness = self._last_brightness_logged.get(led_index, -1)
                if adjusted_brightness != last_brightness:
                    log(_SRC, f"LED {led_index} 💡 Lux {last_lux if last_lux else 0}→{lux} (Δ={abs(lux - (last_lux or 0))}) → Brightness {adjusted_brightness}%")
                    self._last_brightness_logged[led_index] = adjusted_brightness
                
                # Update last processed lux
                self._last_lux_processed[led_index] = lux
            else:
                # Hysteresis active - use last adjusted brightness
                if led_index in self._last_adjusted_brightness:
                    adjusted_brightness = self._last_adjusted_brightness[led_index]

        adjusted_brightness = int(clamp(adjusted_brightness, 0, 100))
        await asyncio.sleep_ms(0)
        return adjusted_rgb, adjusted_brightness

    async def get_sensor_summary(self):
        summary={}
        for k,d in self.sensor_cache.items():
            summary[k]={
                "connected": d.get("connected", False),
                "lux": d.get("luminosity",0),
                "temp": d.get("temperature",0),
                "humidity": d.get("humidity",0),
                "audio": d.get("audio_peak",0),
            }
        return summary
