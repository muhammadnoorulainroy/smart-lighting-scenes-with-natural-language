"""
Sensor Logic - Convert sensor data to LED states
Implements the lighting effects based on environmental sensors
"""

import time
import random

def map_value(x, in_min, in_max, out_min, out_max):
    """Map value from one range to another"""
    return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min

def clamp(value, min_val, max_val):
    """Clamp value between min and max"""
    return max(min_val, min(max_val, value))

def rgb_to_hsv(r, g, b):
    """Convert RGB to HSV"""
    r, g, b = r / 255.0, g / 255.0, b / 255.0
    max_c = max(r, g, b)
    min_c = min(r, g, b)
    diff = max_c - min_c
    
    if max_c == min_c:
        h = 0
    elif max_c == r:
        h = (60 * ((g - b) / diff) + 360) % 360
    elif max_c == g:
        h = (60 * ((b - r) / diff) + 120) % 360
    else:
        h = (60 * ((r - g) / diff) + 240) % 360
    
    s = 0 if max_c == 0 else (diff / max_c)
    v = max_c
    
    return h, s, v

def hsv_to_rgb(h, s, v):
    """Convert HSV to RGB"""
    c = v * s
    x = c * (1 - abs((h / 60) % 2 - 1))
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
    
    return int((r + m) * 255), int((g + m) * 255), int((b + m) * 255)

def kelvin_to_rgb(kelvin):
    """Convert color temperature (Kelvin) to RGB"""
    temp = kelvin / 100
    
    # Calculate red
    if temp <= 66:
        red = 255
    else:
        red = temp - 60
        red = 329.698727446 * (red ** -0.1332047592)
        red = clamp(red, 0, 255)
    
    # Calculate green
    if temp <= 66:
        green = temp
        green = 99.4708025861 * math.log(green) - 161.1195681661
    else:
        green = temp - 60
        green = 288.1221695283 * (green ** -0.0755148492)
    green = clamp(green, 0, 255)
    
    # Calculate blue
    if temp >= 66:
        blue = 255
    elif temp <= 19:
        blue = 0
    else:
        blue = temp - 10
        blue = 138.5177312231 * math.log(blue) - 305.0447927307
        blue = clamp(blue, 0, 255)
    
    return int(red), int(green), int(blue)

class SensorLogic:
    """Apply sensor-based lighting effects"""
    
    def __init__(self, config):
        self.config = config
        
        # Audio flash state
        self.flash_state = {}
        self.last_beat = {}
    
    def apply_luminosity(self, brightness, luminosity):
        """
        Apply luminosity effect (inverse: high lux = dim LEDs)
        
        Args:
            brightness: Base brightness (0-100)
            luminosity: Ambient light level (lux)
        
        Returns:
            Adjusted brightness (0-100)
        """
        if luminosity <= self.config.LUMINOSITY_LOW:
            multiplier = 1.0  # Dark room = full brightness
        elif luminosity >= self.config.LUMINOSITY_HIGH:
            multiplier = self.config.BRIGHTNESS_AT_HIGH / 100.0  # Bright room = dim
        else:
            # Linear interpolation
            multiplier = map_value(
                luminosity,
                self.config.LUMINOSITY_LOW,
                self.config.LUMINOSITY_HIGH,
                1.0,
                self.config.BRIGHTNESS_AT_HIGH / 100.0
            )
        
        return int(brightness * multiplier)
    
    def apply_temperature_color_shift(self, rgb, temperature):
        """
        Apply temperature-based color shift
        Cold → Warm white (orange), Hot → Cool white (blue)
        
        Args:
            rgb: Base color (r, g, b)
            temperature: Temperature in °C
        
        Returns:
            Adjusted RGB (r, g, b)
        """
        # Map temperature to color temperature (Kelvin)
        temp_clamped = clamp(temperature, self.config.TEMP_MIN, self.config.TEMP_MAX)
        color_temp = map_value(
            temp_clamped,
            self.config.TEMP_MIN,
            self.config.TEMP_MAX,
            self.config.COLOR_TEMP_WARM,
            self.config.COLOR_TEMP_COOL
        )
        
        # Get target color from Kelvin
        target_r, target_g, target_b = kelvin_to_rgb(color_temp)
        
        # Blend base RGB with target color (50/50 mix)
        r = int((rgb[0] + target_r) / 2)
        g = int((rgb[1] + target_g) / 2)
        b = int((rgb[2] + target_b) / 2)
        
        return (r, g, b)
    
    def apply_humidity_saturation(self, rgb, humidity):
        """
        Apply humidity-based saturation adjustment
        60% humidity = baseline, higher humidity = more saturation
        
        Args:
            rgb: Base color (r, g, b)
            humidity: Humidity percentage
        
        Returns:
            Adjusted RGB (r, g, b)
        """
        # Convert RGB to HSV
        h, s, v = rgb_to_hsv(*rgb)
        
        # Calculate target saturation based on humidity
        if humidity < self.config.HUMIDITY_MIN:
            target_sat = self.config.SATURATION_MIN / 100.0
        elif humidity > self.config.HUMIDITY_MAX:
            target_sat = self.config.SATURATION_MAX / 100.0
        else:
            # Linear mapping
            target_sat = map_value(
                humidity,
                self.config.HUMIDITY_MIN,
                self.config.HUMIDITY_MAX,
                self.config.SATURATION_MIN / 100.0,
                self.config.SATURATION_MAX / 100.0
            )
        
        # Blend current and target saturation
        new_s = (s + target_sat) / 2
        
        # Convert back to RGB
        return hsv_to_rgb(h, new_s, v)
    
    def check_audio_flash(self, led_index, audio_peak):
        """
        Check if audio peak should trigger flash effect
        
        Args:
            led_index: LED index
            audio_peak: Audio peak value (0-1023)
        
        Returns:
            True if flash should occur
        """
        current_time = time.ticks_ms()
        
        # Check if in flash state
        if led_index in self.flash_state:
            elapsed = time.ticks_diff(current_time, self.flash_state[led_index]['start'])
            if elapsed < self.config.AUDIO_FLASH_DURATION:
                return True  # Still flashing
            else:
                # Flash done
                del self.flash_state[led_index]
                return False
        
        # Check for new beat
        if audio_peak > self.config.AUDIO_THRESHOLD:
            # Check debounce
            if led_index in self.last_beat:
                elapsed = time.ticks_diff(current_time, self.last_beat[led_index])
                if elapsed < self.config.AUDIO_DEBOUNCE:
                    return False  # Too soon, ignore
            
            # Trigger flash - print debug info
            print(f"🎵 AUDIO TRIGGER! LED {led_index} | Peak: {audio_peak} (threshold: {self.config.AUDIO_THRESHOLD})")
            
            self.flash_state[led_index] = {
                'start': current_time,
                'prev_rgb': None,  # Will be set by caller
                'prev_brightness': None
            }
            self.last_beat[led_index] = current_time
            return True
        
        return False
    
    def get_flash_color(self):
        """Get random color for flash effect"""
        return (
            random.randint(100, 255),
            random.randint(100, 255),
            random.randint(100, 255)
        )
    
    def save_flash_previous_state(self, led_index, rgb, brightness):
        """Save state before flash"""
        if led_index in self.flash_state:
            self.flash_state[led_index]['prev_rgb'] = rgb
            self.flash_state[led_index]['prev_brightness'] = brightness
    
    def get_flash_restore_state(self, led_index):
        """Get state to restore after flash"""
        if led_index in self.flash_state:
            return (
                self.flash_state[led_index].get('prev_rgb'),
                self.flash_state[led_index].get('prev_brightness')
            )
        return None, None
    
    def calculate_led_state(self, led_index, base_rgb, base_brightness, sensor_data, mode):
        """
        Calculate final LED state based on sensors
        
        Args:
            led_index: LED index
            base_rgb: Base color (r, g, b)
            base_brightness: Base brightness (0-100)
            sensor_data: Dict with luminosity, temperature, humidity, audio_peak, connected
            mode: 'auto' or 'manual'
        
        Returns:
            (rgb, brightness) tuple
        """
        # Manual mode: return base values
        if mode == 'manual' or not sensor_data['connected']:
            return base_rgb, base_brightness
        
        # Auto mode: apply sensor effects
        final_rgb = base_rgb
        final_brightness = base_brightness
        
        # Check audio flash first (overrides everything)
        if self.check_audio_flash(led_index, sensor_data['audio_peak']):
            # Save previous state
            self.save_flash_previous_state(led_index, base_rgb, base_brightness)
            # Return flash state (change RGB color for 3 seconds)
            return self.get_flash_color(), 15  # Flash at 15% brightness
        
        # Check if we just finished flash (restore previous)
        prev_rgb, prev_brightness = self.get_flash_restore_state(led_index)
        if prev_rgb and prev_brightness:
            # Restore and clear
            if led_index in self.flash_state:
                del self.flash_state[led_index]
            return prev_rgb, prev_brightness
        
        # Apply luminosity (adjust brightness)
        final_brightness = self.apply_luminosity(
            final_brightness,
            sensor_data['luminosity']
        )
        
        # Apply temperature (color shift)
        final_rgb = self.apply_temperature_color_shift(
            final_rgb,
            sensor_data['temperature']
        )
        
        # Apply humidity (saturation)
        final_rgb = self.apply_humidity_saturation(
            final_rgb,
            sensor_data['humidity']
        )
        
        return final_rgb, final_brightness


# Simplified kelvin_to_rgb without math module
def kelvin_to_rgb(kelvin):
    """Simplified Kelvin to RGB conversion"""
    temp = kelvin / 100
    
    # Simplified calculation
    if temp <= 66:
        red = 255
    else:
        red = int(329.7 * ((temp - 60) ** -0.13))
        red = clamp(red, 0, 255)
    
    if temp <= 66:
        green = int(99.5 * ((temp ** 0.19) - 1.6))
    else:
        green = int(288.1 * ((temp - 60) ** -0.08))
    green = clamp(green, 0, 255)
    
    if temp >= 66:
        blue = 255
    elif temp <= 19:
        blue = 0
    else:
        blue = int(138.5 * ((temp - 10) ** 0.18) - 30)
        blue = clamp(blue, 0, 255)
    
    return int(red), int(green), int(blue)


