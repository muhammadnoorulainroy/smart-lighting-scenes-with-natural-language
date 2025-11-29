# Embedded System Setup Tutorial

This tutorial guides you through setting up the Smart Lighting embedded system from hardware assembly to first operation.

## Prerequisites

### Hardware Required
- 2x ESP32 DevKit v1 (or compatible)
- 2x Adafruit Feather nRF52840 Sense
- 1x SH1107 OLED Display (128x64, I2C)
- 1x WS2812B LED Strip (5 LEDs minimum)
- Jumper wires
- USB cables for programming

### Software Required
- Python 3.8+
- esptool (ESP32 flashing)
- Thonny IDE or ampy (MicroPython file transfer)
- CircuitPython 8.x bundle

### Network Required
- WiFi network (2.4GHz)
- MQTT broker (Mosquitto recommended)

## Step 1: Flash Firmware

### ESP32 Devices

1. Download MicroPython firmware:
   ```
   https://micropython.org/download/esp32/
   ```

2. Erase and flash:
   ```bash
   esptool.py --chip esp32 --port COM3 erase_flash
   esptool.py --chip esp32 --port COM3 write_flash -z 0x1000 esp32-20231005-v1.21.0.bin
   ```

3. Verify connection:
   ```bash
   # Open serial terminal at 115200 baud
   # You should see MicroPython REPL prompt >>>
   ```

### nRF52840 Devices

1. Enter bootloader mode:
   - Double-tap reset button
   - Board mounts as USB drive (FTHR840BOOT)

2. Copy CircuitPython UF2:
   - Download from circuitpython.org for Feather nRF52840 Sense
   - Drag UF2 file to FTHR840BOOT drive
   - Board reboots as CIRCUITPY drive

## Step 2: Wire Hardware

### ESP32 #1 (BLE Central)

| ESP32 Pin | Connection      |
|-----------|-----------------|
| GPIO 17   | ESP32 #2 GPIO16 |
| GND       | ESP32 #2 GND    |

### ESP32 #2 (Main Controller)

| ESP32 Pin | Connection         |
|-----------|--------------------|
| GPIO 16   | ESP32 #1 GPIO17    |
| GPIO 13   | WS2812B Data       |
| GPIO 23   | OLED SDA           |
| GPIO 22   | OLED SCL           |
| GPIO 15   | Button A           |
| GPIO 32   | Button B           |
| GPIO 14   | Button C           |
| 3.3V      | OLED VCC, LED VCC  |
| GND       | Common ground      |

### WS2812B LED Strip

| LED Pin | Connection      |
|---------|-----------------|
| DIN     | ESP32 #2 GPIO13 |
| VCC     | 5V or 3.3V      |
| GND     | Common ground   |

## Step 3: Configure MQTT Broker

### Install Mosquitto

```bash
# Ubuntu/Debian
sudo apt install mosquitto mosquitto-clients

# Windows (Chocolatey)
choco install mosquitto

# macOS
brew install mosquitto
```

### Start Broker

```bash
mosquitto -v
```

### Test Connection

```bash
# Terminal 1: Subscribe
mosquitto_sub -h localhost -t "smartlight/#" -v

# Terminal 2: Publish
mosquitto_pub -h localhost -t "smartlight/test" -m "hello"
```

## Step 4: Upload ESP32 #2 Code

1. Edit `embedded/esp32_controller_2/config.py`:

   ```python
   WIFI_SSID = "YourNetworkName"
   WIFI_PASSWORD = "YourPassword"
   MQTT_BROKER = "192.168.1.100"  # Your broker IP
   ```

2. Upload files using Thonny or ampy:

   ```bash
   cd embedded/esp32_controller_2
   
   # Upload all Python files
   ampy --port COM3 put boot.py
   ampy --port COM3 put main.py
   ampy --port COM3 put config.py
   ampy --port COM3 put config_bridge.py
   ampy --port COM3 put config_manager.py
   ampy --port COM3 put mqtt_client_async.py
   ampy --port COM3 put uart_receiver_async.py
   ampy --port COM3 put oled_display_async.py
   ampy --port COM3 put led_controller_async.py
   ampy --port COM3 put sensor_logic_async.py
   ampy --port COM3 put wifi_provisioning.py
   ampy --port COM3 put wifi_helper.py
   ampy --port COM3 put logger.py
   ampy --port COM3 put sh1107.py
   ```

3. Reset the ESP32 or disconnect/reconnect USB.

## Step 5: Upload ESP32 #1 Code

1. Edit `embedded/esp32_controller_1/config.py` if needed (default targets SmartLight-Sensor-1 and SmartLight-Sensor-2).

2. Upload files:

   ```bash
   cd embedded/esp32_controller_1
   
   ampy --port COM4 put boot.py
   ampy --port COM4 put main.py
   ampy --port COM4 put ble_central.py
   ampy --port COM4 put config.py
   ```

## Step 6: Upload nRF52840 Code

1. Connect Feather nRF52840 Sense via USB (appears as CIRCUITPY drive).

2. Copy required libraries to `CIRCUITPY/lib/`:
   - adafruit_bmp280.mpy
   - adafruit_sht31d.mpy
   - adafruit_apds9960/
   - adafruit_ble/

3. Edit device name in `embedded/nrf52840_sensor_1/code.py`:

   ```python
   DEVICE_NAME = "SmartLight-Sensor-1"  # or "SmartLight-Sensor-2"
   ```

4. Copy `code.py` to CIRCUITPY drive root.

5. Repeat for second sensor with different DEVICE_NAME.

## Step 7: First Boot

### Expected Boot Sequence (ESP32 #2)

```
==================================================
SmartLight ESP32 Controller - Boot
==================================================
Free memory: 162800 bytes
[BOOT] Starting main.py...
[ 1s][main] LEDs ready: 5 on GPIO 13
[ 2s][main] Button A ready on GPIO 15
[ 2s][main] WiFi connecting to: YourNetwork
[ 8s][main] WiFi connected: 192.168.1.50
[11s][main] MQTT connected on attempt 1
[11s][main] MQTT ready: 192.168.1.100
[11s][main] UART receiver ready on GPIO 16
[11s][main] Main loop starting...
```

### Expected Boot Sequence (ESP32 #1)

```
==================================================
ESP32 #1 - BLE Sensor Hub
==================================================
[BLE] Central initialized
[BLE] Scanning for 5000ms...
[BLE] Found: SmartLight-Sensor-1 RSSI=-65
[BLE] Connecting to SmartLight-Sensor-1...
[BLE] Connected: SmartLight-Sensor-1
[BLE] Enabled notify for temperature
[BLE] Enabled notify for humidity
[BLE] Enabled notify for luminosity
[BLE] Enabled notify for audio
```

### Expected Boot Sequence (nRF52840)

```
========================================
  SmartLight-Sensor-1
========================================
[SENSOR] BMP280 OK (temp/pressure)
[SENSOR] SHT31 OK (humidity)
[SENSOR] APDS9960 OK (light)
[SENSOR] PDM Mic OK
[BLE] Ready
[BLE] Advertising...
[BLE] Connected!
[INIT] T=22.5C H=45.0% P=1013hPa L=350 A=10
```

## Step 8: Verify Operation

### Check OLED Display
- Home page shows system status
- Press Button B to cycle pages
- Sensor pages show temperature, humidity, light, audio

### Check MQTT Messages

```bash
mosquitto_sub -h localhost -t "smartlight/#" -v
```

Expected output:
```
smartlight/status/online online
smartlight/led/0/state {"rgb":[255,50,0],"brightness":5,"on":true}
smartlight/sensor/SmartLight-Sensor-1 {"temperature":22.5,"humidity":45}
```

### Test LED Control

```bash
# Turn off all lights
mosquitto_pub -h localhost -t "smartlight/command/lights" -m "off"

# Turn on all lights
mosquitto_pub -h localhost -t "smartlight/command/lights" -m "on"

# Set LED 0 color
mosquitto_pub -h localhost -t "smartlight/led/0/color" -m '{"r":0,"g":255,"b":0}'

# Set room brightness
mosquitto_pub -h localhost -t "smartlight/room/Kitchen/brightness" -m "80"
```

## Troubleshooting

### WiFi Connection Failed

1. Verify SSID and password in config.py
2. Ensure 2.4GHz network (ESP32 does not support 5GHz)
3. Check signal strength (move closer to router)
4. Use WiFi provisioning:
   - Hold Button A during boot for 5 seconds
   - Connect to "SmartLight-Setup" AP
   - Navigate to 192.168.4.1
   - Enter WiFi credentials

### MQTT Connection Failed

1. Verify broker IP is correct
2. Ensure broker is running and accessible
3. Check firewall rules (port 1883)
4. Test with mosquitto_pub/sub from ESP32 network

### BLE Sensors Not Found

1. Verify sensor is powered and advertising
2. Check DEVICE_NAME matches TARGET_SENSORS in ESP32 #1
3. Bring sensor closer to ESP32 #1 (within 5 meters)
4. Check for BLE interference (disable other BLE devices)

### ENOMEM Errors

Memory exhaustion during BLE operations:
1. Reduce connected sensor count
2. Increase delays in ble_central.py
3. Call gc.collect() more frequently
4. Restart ESP32 #1

### OLED Not Displaying

1. Verify I2C wiring (SDA/SCL)
2. Check I2C address (default 0x3C)
3. Scan I2C bus:
   ```python
   from machine import I2C, Pin
   i2c = I2C(0, sda=Pin(23), scl=Pin(22))
   print(i2c.scan())
   ```

### LEDs Not Working

1. Verify data pin connection (GPIO 13)
2. Check power supply (3.3V may be insufficient for long strips)
3. Test with simple script:
   ```python
   from machine import Pin
   from neopixel import NeoPixel
   np = NeoPixel(Pin(13), 5)
   np[0] = (255, 0, 0)
   np.write()
   ```

## Factory Reset

Hold Button A for 5 seconds during boot to clear stored WiFi credentials and enter provisioning mode.

## Monitoring

### Serial Console

Connect to ESP32 serial port at 115200 baud to view logs:
```bash
# Windows
putty -serial COM3 -sercfg 115200

# Linux/macOS
screen /dev/ttyUSB0 115200
```

### Memory Status

Heartbeat log every 10 seconds shows free memory:
```
[322s][main] Up:320s Mem:50624B BLE:2/2 MQTT:OK
```

Target: >50KB free memory during normal operation.

