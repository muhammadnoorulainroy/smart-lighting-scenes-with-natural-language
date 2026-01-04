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
        """
        Adjust RGB color based on temperature and humidity.
        Returns: (adjusted_rgb, saturation_pct, color_temp_kelvin)
        """
        adjusted_rgb = base_rgb
        saturation_pct = 100  # Default 100%
        color_temp_kelvin = 4000  # Default neutral white ~4000K
        
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
            
            # Calculate approximate color temperature in Kelvin
            # Map temperature range to Kelvin: cold=6500K (blue), neutral=4000K, hot=2700K (warm)
            temp_norm = (temperature - self.cfg.TEMP_MIN) / (self.cfg.TEMP_MAX - self.cfg.TEMP_MIN)
            temp_norm = clamp(temp_norm, 0.0, 1.0)
            # Inverse: low temp -> high Kelvin (cool), high temp -> low Kelvin (warm)
            color_temp_kelvin = int(lerp(6500, 2700, temp_norm))

        if self.cfg.HUMIDITY_EFFECTS_ENABLED:
            h, s, v = rgb_to_hsv(*adjusted_rgb)

            hum_norm = (humidity - self.cfg.HUMIDITY_MIN) / (self.cfg.HUMIDITY_MAX - self.cfg.HUMIDITY_MIN)
            hum_norm = clamp(hum_norm, 0.0, 1.0)

            target_sat = lerp(self.cfg.SATURATION_AT_MIN_HUMIDITY,
                              self.cfg.SATURATION_AT_MAX_HUMIDITY,
                              hum_norm)
            s = target_sat
            
            # Store saturation as percentage
            saturation_pct = int(target_sat * 100)

            if getattr(self.cfg, "HUE_SHIFT_ENABLED", False):
                hue_shift = (hum_norm - 0.5) * self.cfg.HUE_SHIFT_AMOUNT
                h = (h + hue_shift) % 360

            adjusted_rgb = hsv_to_rgb(h, s, v)

        return adjusted_rgb, saturation_pct, color_temp_kelvin

    async def calculate_led_state(self, led_index, base_rgb, base_brightness):
        sensor_key = None
        for k, scfg in self.cfg.SENSOR_DEVICES.items():
            if scfg["led_index"] == led_index:
                sensor_key = k
                break

        # Get max brightness from config (for capping)
        max_br = self.cfg.MAX_BRIGHTNESS
        if max_br is None:
            max_br = 100
        capped_brightness = min(base_brightness, max_br)
        
        if not sensor_key:
            # No sensor mapped to this LED - return defaults (but respect max brightness)
            return base_rgb, capped_brightness, 100, 4000
            
        if sensor_key not in self.sensor_cache:
            # Sensor mapped but no data yet - return defaults (but respect max brightness)
            return base_rgb, capped_brightness, 100, 4000

        data = self.sensor_cache[sensor_key]
        
        # Check if data is None (sensor found but no data received yet)
        if data is None:
            return base_rgb, capped_brightness, 100, 4000

        # Check if sensor override is enabled (allows sensors to adjust scene values)
        sensor_override = self.cfg.SENSOR_OVERRIDE_ENABLED
        if sensor_override is None:
            sensor_override = True  # Default to enabled
        
        # If sensor override is disabled, don't adjust - just return base values (with MAX_BRIGHTNESS cap)
        if not sensor_override:
            return base_rgb, capped_brightness, 100, 4000

        # Check if disco mode is enabled in config
        disco_enabled = self.cfg.DISCO_ENABLED
        if disco_enabled is None:
            disco_enabled = True  # Default to enabled
        
        # DISCO MODE! 🎉 (3-second rainbow when audio detected)
        # Only continue disco if it's still enabled
        if disco_enabled and self.audio_flash_active.get(led_index):
            elapsed = time.ticks_diff(time.ticks_ms(), self.audio_flash_start.get(led_index, 0))
            if elapsed < self.cfg.AUDIO_DISCO_DURATION:
                # Rainbow disco effect - cycle through colors
                disco_speed = getattr(self.cfg, "AUDIO_DISCO_SPEED", 100)
                hue = (elapsed // disco_speed * 60) % 360  # Change color every 100ms
                disco_rgb = hsv_to_rgb(hue, 1.0, 1.0)  # Full saturation, full brightness
                return disco_rgb, self.cfg.AUDIO_FLASH_BRIGHTNESS, 100, 4000  # Disco: full sat, neutral temp
            self.audio_flash_active[led_index] = False
        elif not disco_enabled:
            # Disco disabled - clear any active flash
            self.audio_flash_active[led_index] = False

        # Trigger new disco mode (audio-reactive lighting)
        audio = data.get("audio", 0)
        if disco_enabled and audio > self.cfg.AUDIO_THRESHOLD:
            self.audio_flash_active[led_index] = True
            self.audio_flash_start[led_index] = time.ticks_ms()
            log(_SRC, f"🎉 DISCO MODE! LED {led_index}: audio={audio}")
            # Return immediate disco effect
            disco_rgb = hsv_to_rgb(0, 1.0, 1.0)  # Start with red
            return disco_rgb, self.cfg.AUDIO_FLASH_BRIGHTNESS, 100, 4000  # Disco: full sat, neutral temp

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

        adjusted_rgb, saturation_pct, color_temp_kelvin = self.adjust_color_for_climate(base_rgb, temp, hum)
        adjusted_brightness = base_brightness

        # Log climate effect only on significant changes
        if adjusted_rgb != base_rgb:
            last_t = self._last_temp_logged.get(led_index)
            last_h = self._last_humidity_logged.get(led_index)
            if last_t is None or last_h is None or abs(temp - last_t) >= 2.0 or abs(hum - last_h) >= 10.0:
                log(_SRC, f"LED{led_index} 🌡️T={temp:.1f}C H={hum:.0f}% {base_rgb}->{adjusted_rgb} Sat={saturation_pct}% CT={color_temp_kelvin}K")
                self._last_temp_logged[led_index] = temp
                self._last_humidity_logged[led_index] = hum

        # Auto-dim based on ambient light (lux)
        auto_dim = self.cfg.AUTO_DIM_ENABLED
        if auto_dim is None:
            auto_dim = True  # Default to enabled
        
        if auto_dim:
            # Lux-based brightness with hysteresis
            # Rule: Use base_brightness (from scene/command) as MAXIMUM
            # Sensor can only REDUCE brightness based on ambient light, not increase
            
            last_lux = self._last_lux_processed.get(led_index, None)
            
            # Check if we should update brightness (hysteresis)
            should_update = False
            if last_lux is None:
                should_update = True  # First time
            elif abs(lux - last_lux) > 20:
                should_update = True  # Lux changed by more than 20
            
            if should_update:
                # Get brightness limits from backend config
                config_max = self.cfg.MAX_BRIGHTNESS
                config_min = self.cfg.MIN_BRIGHTNESS
                
                # Debug: log config values on first update for each LED
                if last_lux is None:
                    log(_SRC, f"LED{led_index} config: MAX_BR={config_max}, MIN_BR={config_min}, base={base_brightness}")
                
                # Ensure valid defaults if config returns None
                if config_max is None:
                    config_max = 100
                if config_min is None:
                    config_min = 0
                
                # Use base_brightness as the scene maximum, but cap by config limit
                scene_base = base_brightness if base_brightness > 0 else 100
                scene_max = min(scene_base, config_max)  # Never exceed config max
                min_br = max(config_min, 0)  # Ensure non-negative
                
                if lux >= self.cfg.LUX_MAX:
                    # Very bright room - use minimum brightness
                    target_brightness_percent = min_br
                elif lux <= self.cfg.LUX_MIN:
                    # Dark room - use scene's brightness (the maximum)
                    target_brightness_percent = scene_max
                else:
                    # Interpolate inversely between min and scene_max
                    lux_factor = (lux - self.cfg.LUX_MIN) / (self.cfg.LUX_MAX - self.cfg.LUX_MIN)
                    # Invert: 0 lux -> scene_max, LUX_MAX -> min_br
                    target_brightness_percent = scene_max - (lux_factor * (scene_max - min_br))
                
                # Quantize to 1% steps
                target_brightness_percent = round(target_brightness_percent)
                target_brightness_percent = int(clamp(target_brightness_percent, min_br, scene_max))
                
                # Use the calculated brightness (capped by scene maximum)
                adjusted_brightness = target_brightness_percent
                
                # Store the adjusted brightness (for hysteresis)
                self._last_adjusted_brightness[led_index] = adjusted_brightness
                
                # Log the change
                last_brightness = self._last_brightness_logged.get(led_index, -1)
                if adjusted_brightness != last_brightness:
                    log(_SRC, f"LED {led_index} 💡 Lux {last_lux if last_lux else 0}->{lux} -> Brightness {adjusted_brightness}% (max {scene_max}%)")
                    self._last_brightness_logged[led_index] = adjusted_brightness
                
                # Update last processed lux
                self._last_lux_processed[led_index] = lux
            else:
                # Hysteresis active - use last adjusted brightness
                if led_index in self._last_adjusted_brightness:
                    adjusted_brightness = self._last_adjusted_brightness[led_index]

        # ALWAYS apply MAX_BRIGHTNESS limit from config (even if auto_dim is off)
        config_max_final = self.cfg.MAX_BRIGHTNESS
        if config_max_final is None:
            config_max_final = 100
        adjusted_brightness = int(clamp(adjusted_brightness, 0, config_max_final))
        
        await asyncio.sleep_ms(0)
        return adjusted_rgb, adjusted_brightness, saturation_pct, color_temp_kelvin

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
