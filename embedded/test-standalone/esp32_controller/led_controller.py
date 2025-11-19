"""
WS2812B LED Controller for ESP32
"""

import neopixel
from machine import Pin
import time

class LEDController:
    def __init__(self, pin, num_leds):
        """Initialize LED controller"""
        self.num_leds = num_leds
        self.np = neopixel.NeoPixel(Pin(pin), num_leds)
        
        # Current state for each LED
        self.states = []
        for i in range(num_leds):
            self.states.append({
                'rgb': (0, 0, 0),
                'brightness': 0,
                'on': False
            })
        
        # Turn off all LEDs initially
        self.clear()
    
    def clear(self):
        """Turn off all LEDs"""
        for i in range(self.num_leds):
            self.np[i] = (0, 0, 0)
        self.np.write()
    
    def set_led(self, index, rgb, brightness, on=True):
        """
        Set a single LED
        
        Args:
            index: LED index (0-4)
            rgb: Tuple (r, g, b) 0-255
            brightness: 0-100 (percentage)
            on: Boolean
        """
        if index < 0 or index >= self.num_leds:
            return
        
        # Save state
        self.states[index] = {
            'rgb': rgb,
            'brightness': brightness,
            'on': on
        }
        
        if not on or brightness == 0:
            self.np[index] = (0, 0, 0)
        else:
            # Apply brightness
            factor = brightness / 100.0
            r = int(rgb[0] * factor)
            g = int(rgb[1] * factor)
            b = int(rgb[2] * factor)
            self.np[index] = (r, g, b)
    
    def update(self):
        """Write changes to LEDs"""
        self.np.write()
    
    def set_all(self, rgb, brightness, on=True):
        """Set all LEDs to same color"""
        for i in range(self.num_leds):
            self.set_led(i, rgb, brightness, on)
        self.update()
    
    def get_state(self, index):
        """Get current state of LED"""
        if index < 0 or index >= self.num_leds:
            return None
        return self.states[index]
    
    def test_pattern(self):
        """Test pattern - cycle through colors"""
        colors = [
            (255, 0, 0),    # Red
            (0, 255, 0),    # Green
            (0, 0, 255),    # Blue
            (255, 255, 0),  # Yellow
            (255, 0, 255),  # Magenta
        ]
        
        for i in range(self.num_leds):
            self.set_led(i, colors[i], 5, True)  # Very low brightness for test
        self.update()
        time.sleep(1)
        
        self.clear()


