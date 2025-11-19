# Smart Lighting Standalone Test

Complete standalone test setup for ESP32 + nRF52840 sensors without backend.

## 📦 **Hardware Requirements**

### **ESP32**
- ESP32 DevKit (any variant with BLE)
- 5× WS2812B RGB LEDs
- 128×64 OLED display (I2C, SH1107 or SSD1306)
- Breadboard and jumper wires
- 330Ω resistor (for LED data line)
- 1000µF capacitor (for LED power stabilization)
- 5V power supply (for LEDs)

### **nRF52840** (×2)
- 2× Adafruit Bluefruit Sense (nRF52840)
- USB cables for programming

---

## 🔌 **Wiring Diagram**

### **ESP32 Connections**

```
ESP32               Component
────────────────────────────────────
GPIO 13      ─────► WS2812B Data In (through 330Ω)
GPIO 21      ─────► OLED SDA
GPIO 22      ─────► OLED SCL
GND          ─────► OLED GND, LED GND
3.3V         ─────► OLED VCC
5V           ─────► LED 5V (with 1000µF cap)
```

### **WS2812B LED Strip**

```
5V ─────┬──────► LED VCC (+)
        │
      [1000µF]  (Capacitor +/- across power)
        │
GND ────┴──────► LED GND (-)

ESP32 GPIO 13 ──[330Ω]──► LED Data In

LED Chain: LED0 → LED1 → LED2 → LED3 → LED4
```

### **OLED Display**

```
OLED Pin        ESP32
─────────────────────
VCC     ──────► 3.3V
GND     ──────► GND
SDA     ──────► GPIO 21
SCL     ──────► GPIO 22
```

---

## 💾 **Software Setup**

### **1. Install Required Tools**

#### **For nRF52840 (CircuitPython)**

1. **Download CircuitPython:**
   - Go to: https://circuitpython.org/board/nrf52840_feather_sense/
   - Download latest `.uf2` file

2. **Flash CircuitPython:**
   - Connect nRF52840 via USB
   - Double-click reset button (LED will pulse)
   - Drag `.uf2` file to `FEATHERBOOT` drive
   - Device will reboot as `CIRCUITPY` drive

3. **Install Libraries:**
   ```bash
   # Download Adafruit CircuitPython Bundle
   # https://github.com/adafruit/Adafruit_CircuitPython_Bundle/releases
   
   # Copy these libraries to CIRCUITPY/lib/:
   - adafruit_apds9960
   - adafruit_bmp280.mpy
   - adafruit_sht31d.mpy
   - adafruit_ble (entire folder)
   ```

#### **For ESP32 (MicroPython)**

1. **Install VS Code + Pymakr Extension:**
   - Install VS Code: https://code.visualstudio.com/
   - Install Pymakr extension in VS Code

2. **Flash MicroPython:**
   - Download latest ESP32 firmware: https://micropython.org/download/esp32/
   - Install esptool: `pip install esptool`
   - Erase flash:
     ```bash
     esptool.py --port COM3 erase_flash
     ```
   - Flash MicroPython:
     ```bash
     esptool.py --chip esp32 --port COM3 write_flash -z 0x1000 esp32-xxxxxxxx.bin
     ```

3. **Install MicroPython Libraries:**
   
   The ESP32 code uses these built-in/standard libraries:
   - `neopixel` (built-in)
   - `bluetooth` / `ubluetooth` (built-in)
   - `machine` (built-in)
   
   **Install `sh1107` library (for SH1107 OLED displays):**
   - File `sh1107.py` is already included in the project folder
   - Upload to ESP32 root directory using Pymakr
   - Source: https://github.com/peter-l5/SH1107
   
   **Note:** If you have an SSD1306 display instead, download and use `ssd1306.py`

---

## 📂 **Project Files**

### **For nRF52840 Sensor #1**

1. Copy `nrf52840_sensor_1/code.py` to `CIRCUITPY/code.py` on first nRF52840
2. Eject drive safely
3. Device will reboot and start advertising as `SmartLight-Sensor-1`

### **For nRF52840 Sensor #2**

1. Copy `nrf52840_sensor_2/code.py` to `CIRCUITPY/code.py` on second nRF52840
2. Eject drive safely
3. Device will reboot and start advertising as `SmartLight-Sensor-2`

### **For ESP32**

1. Open `esp32_controller/` folder in VS Code
2. Connect ESP32 via USB
3. In Pymakr terminal, click "Upload" (uploads all `.py` files)
4. Click "Reset device" or press EN button on ESP32

---

## 🚀 **Running the Test**

### **Step-by-Step Startup**

1. **Power on nRF52840 sensors** (both)
   - Connect via USB or battery
   - Check serial output: Should show "Waiting for ESP32 to connect..."

2. **Power on ESP32**
   - Connect via USB
   - LEDs should show test pattern briefly
   - OLED should show "Initializing..."

3. **Wait for BLE connection** (5-10 seconds)
   - Serial output: "Found SmartLight-Sensor-1..."
   - Serial output: "Connected to ESP32!"
   - OLED shows "BLE:2/2" when both connected

4. **Verify sensor data**
   - Cover light sensor → LEDs should brighten
   - Breathe on sensor → Temperature/humidity change
   - Clap or make loud noise → LED should flash random color

---

## 🎨 **Expected Behavior**

### **LED Mapping**

| LED | Room Name | Has Sensor? | Behavior |
|-----|-----------|-------------|----------|
| 0   | Living    | ✅ Yes      | Sensor-reactive (nRF #1) |
| 1   | Bedroom   | ✅ Yes      | Sensor-reactive (nRF #2) |
| 2   | Kitchen   | ❌ No       | Static warm white (80%) |
| 3   | Bath      | ❌ No       | Static pink (50%) |
| 4   | Hallway   | ❌ No       | Static warm white (60%) |

### **Sensor Effects (AUTO Mode)**

**LED 0 & LED 1 (with sensors):**

1. **Luminosity (inverse):**
   - Bright room (800+ lux) → LED dims to ~20%
   - Dark room (< 50 lux) → LED brightens to 100%

2. **Temperature (color shift):**
   - Cold (15°C) → Warm white (orange tones)
   - Hot (30°C) → Cool white (blue tones)

3. **Humidity (saturation):**
   - Low humidity (< 60%) → Pale colors (low saturation)
   - High humidity (> 70%) → Vibrant colors (high saturation)

4. **Audio (flash effect):**
   - Loud sound (> threshold) → Random color at 100% for 1 second
   - Returns to previous state after flash

**LED 2, 3, 4 (no sensors):**
- Static colors, no sensor effects

### **OLED Pages (auto-rotate not implemented yet)**

Press button to cycle pages (not implemented in this test):

- **Page 0:** Home overview (all 5 rooms, BLE status)
- **Page 1:** Living room detail (sensors + LED state)
- **Page 2:** Bedroom detail (sensors + LED state)
- **Page 3:** System status (BLE, uptime, mode)

---

## 🐛 **Troubleshooting**

### **Issue: LEDs don't light up**

- Check power supply (5V, sufficient amperage)
- Check GPIO 13 connection (with 330Ω resistor)
- Check GND connection between ESP32 and LEDs
- Verify capacitor polarity (+ to 5V, - to GND)

### **Issue: OLED doesn't work**

- Check I2C address (run I2C scanner: `i2c.scan()`)
  - SH1107: typically 0x3C (60) or 0x3D (61)
  - Update `addr` in config.py if different
- Verify SDA/SCL connections (GPIO 21/22)
- Check power (OLED VCC to 3.3V, not 5V!)
- Ensure `sh1107.py` is uploaded to ESP32 root directory
- If you have SSD1306 display instead, modify `oled_display.py` to import `ssd1306`

### **Issue: BLE sensors don't connect**

- Check nRF52840 serial output (should show "Waiting...")
- Restart ESP32 (BLE scan runs every 5 seconds)
- Check sensor names match in `config.py`:
  - `SmartLight-Sensor-1`
  - `SmartLight-Sensor-2`
- Try connecting only one sensor first

### **Issue: Sensors connected but no effect**

- Check mode: Should be `'auto'` in `config.py`
- Verify sensor data in serial output
- Check thresholds in `config.py` (may need adjustment)
- Cover light sensor completely → LEDs should brighten

### **Issue: Audio flash doesn't work**

- Clap loudly near nRF52840 microphone
- Check `AUDIO_THRESHOLD` in `config.py` (lower if too sensitive)
- Verify audio data in serial: `Audio=XXX` (should spike)

### **Issue: Memory errors on ESP32**

- Run `gc.collect()` manually
- Reduce `NUM_LEDS` if using fewer LEDs
- Disable OLED if not needed (`oled_display.enabled = False`)

---

## 📊 **Serial Monitor Output**

### **Expected ESP32 Output:**

```
========================================
Smart Lighting Controller v1.0
Standalone Test Mode
========================================

[1/4] Initializing LEDs...
  → 5 LEDs on GPIO 13

[2/4] Initializing OLED...
OLED initialized
  → 128x64 at 0x3C

[3/4] Initializing BLE...
  → Scanning for 2 sensors

[4/4] Initializing sensor logic...
  → Sensor effects configured

========================================
System Ready!
========================================

Room Configuration:
  LED 0: Living (SENSOR)
  LED 1: Bedroom (SENSOR)
  LED 2: Kitchen
  LED 3: Bath
  LED 4: Hallway

Mode: AUTO
========================================

Scanning for BLE devices...
Found SmartLight-Sensor-1 at AA:BB:CC:DD:EE:F1
Connected to SmartLight-Sensor-1!
SmartLight-Sensor-1: Found TX characteristic
SmartLight-Sensor-1: Ready to receive data

Found SmartLight-Sensor-2 at AA:BB:CC:DD:EE:F2
Connected to SmartLight-Sensor-2!
SmartLight-Sensor-2: Found TX characteristic
SmartLight-Sensor-2: Ready to receive data

[40] BLE: 2/2 | Mode: auto | Lights: ON | Page: 0
  sensor_1: Lux=350, Temp=23.2°C, Hum=68.5%, Audio=45
  sensor_2: Lux=280, Temp=22.8°C, Hum=70.1%, Audio=32
```

### **Expected nRF52840 Output:**

```
Initializing SmartLight-Sensor-1...
BLE Name: SmartLight-Sensor-1
Sensors initialized!
Starting BLE advertising...
Waiting for ESP32 to connect...
Connected to ESP32!
Sent: 350,23.2,68.5,45
Sent: 348,23.3,68.6,42
Sent: 355,23.2,68.5,48
```

---

## 🧪 **Testing Checklist**

- [ ] All 5 LEDs light up
- [ ] OLED displays home overview
- [ ] BLE sensors connect (both show connected)
- [ ] Cover light sensor → LEDs brighten (Living & Bedroom)
- [ ] Uncover sensor → LEDs dim
- [ ] Breathe on sensor → Color shifts slightly (temperature/humidity)
- [ ] Clap loudly → LED flashes random color for 1 second
- [ ] Serial output shows sensor data updating

---

## 🎯 **Next Steps**

Once standalone test works:

1. Add potentiometer (global brightness control)
2. Add button (on/off toggle)
3. Add MQTT connectivity (to backend)
4. Implement OLED button controls (page cycling)
5. Add WiFi configuration

---

## 📖 **Configuration Options**

Edit `config.py` to adjust:

- **LED count:** `NUM_LEDS`
- **LED pin:** `LED_PIN`
- **Room names:** `SENSOR_DEVICES`, `STATIC_ROOMS`
- **Sensor thresholds:** `LUMINOSITY_HIGH/LOW`, `AUDIO_THRESHOLD`
- **Color temperature range:** `TEMP_MIN/MAX`, `COLOR_TEMP_WARM/COOL`
- **Humidity saturation:** `HUMIDITY_MIN/MAX`, `SATURATION_MIN/MAX`
- **Update rates:** `UPDATE_RATE`, `OLED_UPDATE_RATE`

---

## 📝 **Notes**

- **Power:** WS2812B LEDs can draw up to 60mA per LED at full white. 5 LEDs × 60mA = 300mA. Use appropriate power supply.
- **BLE Range:** Keep nRF52840 sensors within 5-10 meters of ESP32.
- **Performance:** System runs at 20Hz (50ms update rate) with OLED updates at 5Hz.
- **Memory:** ESP32 with ~100KB free RAM should handle this comfortably.

---

## ✅ **Success Criteria**

System is working correctly if:

1. ✅ All LEDs light up with correct colors
2. ✅ OLED shows room names and BLE status (2/2)
3. ✅ Sensor data updates in serial output (every 500ms)
4. ✅ LEDs 0 & 1 react to light changes (brighten in dark, dim in light)
5. ✅ Audio peaks trigger color flash effect
6. ✅ System runs continuously without crashes

---

**Version:** 1.0  
**Last Updated:** 2025-11-19  
**Status:** ✅ Ready for Testing

