# Embedded System Documentation

## System Architecture

The Smart Lighting embedded system uses a dual-ESP32 architecture with nRF52840 BLE sensors.

```
┌─────────────────┐      BLE      ┌─────────────────┐
│  nRF52840       │◄────────────► │  ESP32 #1       │
│  Sensor 1       │               │  BLE Central    │
└─────────────────┘               │                 │
                                  │    UART TX      │
┌─────────────────┐      BLE      │    GPIO 17      │
│  nRF52840       │◄────────────► │                 │
│  Sensor 2       │               └────────┬────────┘
└─────────────────┘                        │
                                           │ UART (115200 baud)
                                           │
                              ┌────────────▼────────┐
                              │  ESP32 #2           │
                              │  Main Controller    │
                              │                     │
                              │  - WiFi/MQTT        │
                              │  - OLED Display     │
                              │  - WS2812B LEDs     │
                              │  - Button Input     │
                              │  - Sensor Logic     │
                              └─────────────────────┘
```

## Hardware Components

### ESP32 #1 - BLE Central
- **Role**: BLE scanner and sensor data aggregator
- **Connections**:
  - UART TX: GPIO 17 (to ESP32 #2)
- **Firmware**: MicroPython

### ESP32 #2 - Main Controller
- **Role**: System controller, MQTT bridge, display manager
- **Connections**:
  - UART RX: GPIO 16 (from ESP32 #1)
  - WS2812B LED: GPIO 13
  - OLED SDA: GPIO 23
  - OLED SCL: GPIO 22
  - Button A: GPIO 15
  - Button B: GPIO 32
  - Button C: GPIO 14
- **Firmware**: MicroPython

### nRF52840 Sensors (Adafruit Feather Sense)
- **Role**: Environmental sensing
- **Onboard Sensors**:
  - BMP280: Temperature, Pressure
  - SHT31: Humidity
  - APDS9960: Ambient Light
  - PDM Microphone: Audio Level
- **Firmware**: CircuitPython

## Communication Protocols

### BLE (ESP32 #1 ↔ Sensors)

Uses standard Bluetooth SIG Environmental Sensing Service (0x181A).

| Characteristic | UUID   | Data Format            |
|----------------|--------|------------------------|
| Temperature    | 0x2A6E | Int16 (value * 100)    |
| Humidity       | 0x2A6F | Uint16 (value * 100)   |
| Luminosity     | 0x2A77 | Uint16 (raw lux)       |
| Audio Level    | 0x2A78 | Uint16 (0-100 scale)   |

Connection parameters:
- Max concurrent connections: 2
- Notification-based updates (no polling)
- Reconnection on disconnect

### UART (ESP32 #1 → ESP32 #2)

Unidirectional serial communication at 115200 baud.

**Sensor Data Format (compact JSON)**:
```json
{"id":"1","idx":0,"t":22.5,"h":45.0,"l":350,"a":15}
```

| Field | Description          |
|-------|----------------------|
| id    | Sensor ID ("1", "2") |
| idx   | LED index            |
| t     | Temperature (°C)     |
| h     | Humidity (%)         |
| l     | Luminosity (lux)     |
| a     | Audio level (0-100)  |

**Status Message Format**:
```json
{"type":"s","c":2,"r":2,"e":2,"list":"1:1,2:1"}
```

| Field | Description              |
|-------|--------------------------|
| type  | "s" for status           |
| c     | Connected sensor count   |
| r     | Ready sensor count       |
| e     | Expected sensor count    |
| list  | Sensor status list       |

### MQTT (ESP32 #2 ↔ Broker)

Topic structure: `smartlight/<category>/<target>/<action>`

**Subscribed Topics**:
| Topic Pattern              | Description              |
|----------------------------|--------------------------|
| smartlight/command/#       | System commands          |
| smartlight/led/#           | Per-LED control          |
| smartlight/room/#          | Per-room control         |
| smartlight/scene/#         | Scene activation         |

**Published Topics**:
| Topic                           | Description           | Retain |
|---------------------------------|-----------------------|--------|
| smartlight/status/online        | Online status         | Yes    |
| smartlight/led/{n}/state        | LED state             | Yes    |
| smartlight/sensor/{name}        | Sensor data           | No     |
| smartlight/system/state         | System state          | Yes    |

**Command Payloads**:
| Command                    | Payload Example                    |
|----------------------------|------------------------------------|
| smartlight/command/lights  | "on", "off", "toggle"              |
| smartlight/command/mode    | "auto", "manual"                   |
| smartlight/led/0/power     | "on", "off"                        |
| smartlight/led/0/brightness| "50" (0-100)                       |
| smartlight/led/0/color     | {"r":255,"g":100,"b":50}           |
| smartlight/room/Kitchen/power | "on", "off"                     |

## Software Modules

### ESP32 #2 Modules

| File                      | Description                           |
|---------------------------|---------------------------------------|
| main.py                   | Application entry point, main loop    |
| config.py                 | Configuration constants               |
| boot.py                   | Boot sequence, provisioning check     |
| mqtt_client_async.py      | Async MQTT client wrapper             |
| uart_receiver_async.py    | UART message parser                   |
| oled_display_async.py     | OLED display manager (SH1107)         |
| led_controller_async.py   | WS2812B LED controller                |
| sensor_logic_async.py     | Environmental effects processor       |
| wifi_provisioning.py      | WiFi captive portal                   |
| config_manager.py         | NVS configuration storage             |
| config_bridge.py          | Configuration loader                  |
| logger.py                 | Timestamped logging utility           |
| sh1107.py                 | SH1107 OLED driver                    |

### ESP32 #1 Modules

| File                      | Description                           |
|---------------------------|---------------------------------------|
| main.py                   | Entry point                           |
| ble_central.py            | BLE central with state machine        |
| boot.py                   | Boot sequence                         |
| config.py                 | Target sensor configuration           |

### nRF52840 Modules

| File     | Description                              |
|----------|------------------------------------------|
| code.py  | CircuitPython main (runs on boot)        |

## Sensor Effects

### Temperature → Color Temperature
Maps ambient temperature to LED color warmth.

| Temperature | Color Effect     |
|-------------|------------------|
| < 20°C      | Cool blue        |
| 20-24°C     | Neutral white    |
| > 28°C      | Warm orange-red  |

Configuration:
- TEMP_MIN: 20.0°C
- TEMP_MAX: 28.0°C
- TEMP_BLEND_STRENGTH: 0.95

### Humidity → Saturation
Maps humidity to color saturation.

| Humidity | Saturation Effect |
|----------|-------------------|
| < 30%    | Desaturated       |
| 30-70%   | Linear scaling    |
| > 70%    | Full saturation   |

### Luminosity → Brightness
Inverse relationship for ambient light compensation.

| Room Light | LED Brightness |
|------------|----------------|
| Dark       | Maximum        |
| Bright     | Minimum        |

Hysteresis: 10 lux threshold to prevent flickering.

### Audio → Disco Mode
Triggers multicolor flash sequence on loud sounds.

| Parameter            | Value           |
|----------------------|-----------------|
| Threshold            | 25 (0-100)      |
| Duration             | 3000ms          |
| Flash Speed          | 100ms per color |

## Power Management

### OLED Auto-Sleep
- Timeout: 15 seconds of inactivity
- Wake triggers: Button press, MQTT command
- Implementation: Hardware power off (`display.poweroff()`)

### WiFi Power Save
- Mode: MIN_MODEM (sleep between beacons)
- Savings: ~10-15mA

### BLE Optimization
- Notification-based (no polling)
- Event-driven data transfer
- Sequential GATT operations to prevent ENOMEM

## Configuration Reference

### Network Settings
```python
WIFI_SSID = "NetworkName"
WIFI_PASSWORD = "password"
WIFI_TIMEOUT = 20  # seconds

MQTT_BROKER = "192.168.1.100"
MQTT_PORT = 1883
MQTT_CLIENT_ID = "esp32-smartlight"
MQTT_USER = None
MQTT_PASSWORD = None
```

### LED Configuration
```python
NUM_LEDS = 5
LED_PIN = 13
DEFAULT_BRIGHTNESS = 5
MIN_BRIGHTNESS = 0
MAX_BRIGHTNESS = 10

LED_ROOM_NAMES = ["Living Room", "Bedroom", "Kitchen", "Bath", "Hallway"]
LED_SENSOR_MAPPING = {
    0: "SmartLight-Sensor-2",
    1: "SmartLight-Sensor-1",
}
```

### Sensor Thresholds
```python
TEMP_MIN = 20.0
TEMP_MAX = 28.0
HUMIDITY_MIN = 30.0
HUMIDITY_MAX = 70.0
LUX_MIN = 0
LUX_MAX = 1200
AUDIO_THRESHOLD = 25
```

## Error Handling

### BLE ENOMEM Prevention
ESP32 BLE stack has limited buffers. The system uses:
1. State machine for sequential GATT operations
2. 100-300ms delays between characteristic writes
3. `gc.collect()` before each BLE operation
4. Retry logic with exponential backoff

### UART Data Integrity
- Newline-delimited JSON
- Buffer management for partial messages
- Graceful handling of malformed data
- Sensor data merging (preserves values not in update)

### MQTT Reconnection
- Automatic reconnection on disconnect
- Last Will Testament for offline detection
- Retained messages for state persistence

## Memory Management

Target free memory: >50KB during operation

Strategies:
- Periodic `gc.collect()`
- Compact JSON format for UART
- Stale sensor cleanup (30s timeout)
- Minimal logging in production

