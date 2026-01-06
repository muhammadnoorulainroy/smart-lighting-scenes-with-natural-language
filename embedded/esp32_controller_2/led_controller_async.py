# led_controller_async.py
"""
Async LED controller for NeoPixel strip.
- set_led() stores desired state
- update() applies state to hardware
- test_pattern() for boot verification
"""

import uasyncio as asyncio
import neopixel
from machine import Pin
from logger import log, log_err

_SRC = "led_controller_async.py"

class AsyncLEDController:
    def __init__(self, pin, num_leds):
        self.pin = pin
        self.num_leds = num_leds
        self.np = neopixel.NeoPixel(Pin(pin, Pin.OUT), num_leds)
        self.leds = [(0, 0, 0)] * num_leds
        self.brightness = [0] * num_leds
        self.enabled = [False] * num_leds

    async def set_led(self, index, rgb, brightness, on=True):
        if 0 <= index < self.num_leds:
            self.leds[index] = tuple(rgb)
            self.brightness[index] = max(0, min(100, int(brightness)))
            self.enabled[index] = bool(on)
        await asyncio.sleep_ms(0)

    async def update(self):
        for i in range(self.num_leds):
            if self.enabled[i]:
                r, g, b = self.leds[i]
                bright = self.brightness[i] / 100.0
                self.np[i] = (int(r * bright), int(g * bright), int(b * bright))
            else:
                self.np[i] = (0, 0, 0)
        # Yield before blocking WS2812 write (disables interrupts)
        await asyncio.sleep_ms(0)
        try:
            self.np.write()  # Blocking bit-bang (~150µs per LED)
        except OSError as e:
            # RMT peripheral can fail after heavy BLE activity - try to recover
            if "ESP_FAIL" in str(e) or "ESP_ERR_INVALID_STATE" in str(e):
                # Reinitialize NeoPixel
                try:
                    from machine import Pin
                    import neopixel
                    self.np = neopixel.NeoPixel(Pin(self.pin), self.num_leds)
                except:
                    pass  # If reinit fails, skip this update
            raise  # Re-raise to let caller handle
        await asyncio.sleep_ms(0)  # Yield after write

    def clear(self):
        for i in range(self.num_leds):
            self.np[i] = (0, 0, 0)
        self.np.write()

    async def test_pattern(self):
        log(_SRC, "Running LED test pattern...")
        colors = [(255,0,0), (0,255,0), (0,0,255), (255,255,0), (0,255,255)]
        for i in range(min(self.num_leds, len(colors))):
            await self.set_led(i, colors[i], 20, True)
            await self.update()
            await asyncio.sleep_ms(120)
        await asyncio.sleep_ms(300)
        for i in range(self.num_leds):
            await self.set_led(i, (0,0,0), 0, False)
        await self.update()

    def get_state(self, index):
        if 0 <= index < self.num_leds:
            return {
                "rgb": self.leds[index],
                "brightness": self.brightness[index],
                "on": self.enabled[index],
            }
        return None