# Embedded System Documentation

## System Architecture

The Smart Lighting embedded system uses a dual-ESP32 architecture with nRF52840 BLE sensors and dynamic configuration from the backend.

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
                              │  - Runtime Config   │ ◄── Backend MQTT
                              └─────────────────────┘
```

## Hardware Components

### ESP32 #1 - BLE Central
- **Role**: BLE scanner and sensor data aggregator
- **Connections**:
  - UART TX: GPIO 17 (to ESP32 #2)
- **Firmware**: MicroPython
- **Power Mode**: Light sleep between BLE notifications (2s intervals)

### ESP32 #2 - Main Controller
- **Role**: System controller, MQTT bridge, display manager, runtime config
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

| Characteristic | UUID   | Official Name | Data Format            |
|----------------|--------|---------------|------------------------|
| Temperature    | 0x2A6E | Temperature   | Int16 (value × 100)    |
| Humidity       | 0x2A6F | Humidity      | Uint16 (value × 100)   |
| Illuminance    | 0x2AFB | Illuminance   | Uint16 (raw lux)       |
| Audio Level    | 0x2BE4 | Noise         | Uint16 (0-100 scale)   |

Connection parameters:
- Max concurrent connections: 2
- Notification-based updates (no polling)
- Reconnection on disconnect

### UART (ESP32 #1 -> ESP32 #2)

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

Topic structure: `smartlighting/<category>/<target>/<action>`

**Subscribed Topics**:
| Topic Pattern                | Description              |
|------------------------------|--------------------------|
| smartlighting/command/#      | System commands          |
| smartlighting/led/#          | Per-LED control          |
| smartlighting/room/#         | Per-room control         |
| smartlighting/scene/#        | Scene activation         |
| smartlighting/config/#       | Runtime configuration    |

**Published Topics**:
| Topic                           | Description           | Retain |
|---------------------------------|-----------------------|--------|
| smartlighting/status/online     | Online status         | Yes    |
| smartlighting/led/{n}/state     | LED state             | Yes    |
| smartlighting/sensor/{name}     | Sensor data           | No     |
| smartlighting/system/state      | System state          | Yes    |

**Command Payloads**:
| Command                      | Payload Example                    |
|------------------------------|------------------------------------|
| smartlighting/command/lights | "on", "off", "toggle"              |
| smartlighting/command/mode   | "auto", "manual"                   |
| smartlighting/led/0/power    | "on", "off"                        |
| smartlighting/led/0/brightness| "50" (0-100)                      |
| smartlighting/led/0/color    | {"r":255,"g":100,"b":50}           |
| smartlighting/room/Kitchen/power | "on", "off"                     |
| smartlighting/scene/apply    | {"sceneName":"relax","target":"bedroom"} |

**Configuration Topics**:
| Topic                         | Payload Example                   |
|-------------------------------|-----------------------------------|
| smartlighting/config/lighting | {"maxBrightness":80,"autoDimEnabled":true} |
| smartlighting/config/climate  | {"tempMin":20,"tempMax":28}       |
| smartlighting/config/audio    | {"discoEnabled":true,"audioThreshold":25} |
| smartlighting/config/display  | {"showTime":true,"oledTimeout":15} |

## Software Modules

### ESP32 #2 Modules

| File                      | Description                           |
|---------------------------|---------------------------------------|
| main.py                   | Application entry point, main loop    |
| config.py                 | Static configuration defaults         |
| runtime_config.py         | **NEW**: Dynamic config from backend  |
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
| ble_central.py            | BLE central with state machine, light sleep |
| boot.py                   | Boot sequence                         |
| config.py                 | Target sensor configuration           |

### nRF52840 Modules

| File     | Description                              |
|----------|------------------------------------------|
| code.py  | CircuitPython main (runs on boot)        |

## Runtime Configuration

The ESP32 can receive dynamic configuration updates from the backend via MQTT, allowing settings to be changed without reflashing.

### RuntimeConfig Class

The `runtime_config.py` module provides a wrapper that:
1. Loads defaults from `config.py`
2. Applies backend overrides received via MQTT
3. Handles unit conversions (e.g., percentage 0-100 to decimal 0-1)

```python
from runtime_config import RuntimeConfig

cfg = RuntimeConfig()

# Access settings (uses backend value if available, else config.py default)
max_brightness = cfg.MAX_BRIGHTNESS
auto_dim_enabled = cfg.AUTO_DIM_ENABLED
```

### Configuration Categories

#### Lighting
| Setting             | Backend Key          | Type    | Default | Description                    |
|---------------------|----------------------|---------|---------|--------------------------------|
| Global Mode         | globalMode           | string  | "auto"  | "auto" or "manual"             |
| Auto Dim            | autoDimEnabled       | bool    | true    | Lux-based brightness           |
| Sensor Override     | sensorOverrideEnabled| bool    | true    | Allow sensors to adjust scenes |
| Min Brightness      | minBrightness        | int     | 0       | Minimum brightness %           |
| Max Brightness      | maxBrightness        | int     | 100     | Maximum brightness %           |
| Lux Min             | luxMin               | int     | 0       | Dark room threshold            |
| Lux Max             | luxMax               | int     | 1200    | Bright room threshold          |

#### Climate
| Setting             | Backend Key            | Type  | Default | Description                    |
|---------------------|------------------------|-------|---------|--------------------------------|
| Temp Min            | tempMin                | float | 20.0    | Cold color threshold (°C)      |
| Temp Max            | tempMax                | float | 28.0    | Warm color threshold (°C)      |
| Blend Strength      | tempBlendStrength      | int   | 95      | Color blend intensity (%)      |
| Humidity Min        | humidityMin            | int   | 30      | Low humidity threshold         |
| Humidity Max        | humidityMax            | int   | 70      | High humidity threshold        |
| Saturation Min      | saturationAtMinHumidity| int   | 15      | Saturation at low humidity (%) |
| Saturation Max      | saturationAtMaxHumidity| int   | 100     | Saturation at high humidity (%) |

#### Audio
| Setting             | Backend Key          | Type  | Default | Description                    |
|---------------------|----------------------|-------|---------|--------------------------------|
| Disco Enabled       | discoEnabled         | bool  | true    | Enable disco mode              |
| Audio Threshold     | audioThreshold       | int   | 25      | Trigger sensitivity (0-100)    |
| Disco Duration      | discoDuration        | int   | 3000    | Effect duration (ms)           |
| Disco Speed         | discoSpeed           | int   | 100     | Flash interval (ms)            |
| Flash Brightness    | flashBrightness      | int   | 100     | Brightness during disco (%)    |

#### Display
| Setting             | Backend Key          | Type  | Default | Description                    |
|---------------------|----------------------|-------|---------|--------------------------------|
| Auto Sleep          | oledAutoSleep        | bool  | true    | Power save for OLED            |
| Timeout             | oledTimeout          | int   | 15      | Sleep timeout (seconds)        |
| Show Time           | showTime             | bool  | true    | Display clock on home screen   |
| Show Sensor Data    | showSensorData       | bool  | true    | Display readings on pages      |

### Unit Conversion

The backend sends values in user-friendly formats (percentages), which are automatically converted to ESP32 internal formats:

| Backend Value | ESP32 Internal | Example                     |
|---------------|----------------|-----------------------------|
| Saturation 60%| 0.60           | saturationAtMinHumidity     |
| Blend 95%     | 0.95           | tempBlendStrength           |

### Config Request on Boot

On startup, ESP32 #2 requests the full configuration from the backend:
```python
# Publishes to request current config
mqtt.publish("smartlighting/config/request", "full")
```

The backend responds with all categories via the config topics.

## Sensor Effects

### Temperature -> Color Temperature
Maps ambient temperature to LED color warmth.

| Temperature | Color Effect     |
|-------------|------------------|
| < 20°C      | Cool blue        |
| 20-24°C     | Neutral white    |
| > 28°C      | Warm orange-red  |

Configuration:
- TEMP_MIN: 20.0°C (configurable via backend)
- TEMP_MAX: 28.0°C (configurable via backend)
- TEMP_BLEND_STRENGTH: 0.95 (configurable via backend)

### Humidity -> Saturation
Maps humidity to color saturation.

| Humidity | Saturation Effect |
|----------|-------------------|
| < 30%    | Desaturated       |
| 30-70%   | Linear scaling    |
| > 70%    | Full saturation   |

### Luminosity -> Brightness
Inverse relationship for ambient light compensation.

| Room Light | LED Brightness |
|------------|----------------|
| Dark       | Maximum        |
| Bright     | Minimum        |

Hysteresis: 10 lux threshold to prevent flickering.

### Sensor Override (SENSOR_OVERRIDE_ENABLED)

When enabled (default), sensors can adjust scene values:
- Scene sets base brightness
- Sensors can dim based on ambient light
- MAX_BRIGHTNESS cap always applies

When disabled:
- Scenes set exact values
- No sensor-based adjustments
- MAX_BRIGHTNESS cap still applies

### Audio -> Disco Mode
Triggers multicolor flash sequence on loud sounds.

| Parameter            | Value           | Configurable |
|----------------------|-----------------|--------------|
| Threshold            | 25 (0-100)      | Yes          |
| Duration             | 3000ms          | Yes          |
| Flash Speed          | 100ms per color | Yes          |
| Flash Brightness     | 100%            | Yes          |

Disco mode can be disabled via backend configuration.

## Operating Modes

### Auto Mode (Default)
- Sensors actively adjust lighting
- Temperature affects color warmth
- Humidity affects saturation
- Ambient light affects brightness
- Scenes set base values that sensors modify

### Manual Mode
- Sensors do not adjust lighting
- User/scene settings are applied exactly
- Useful for specific lighting needs
- Switch via MQTT: `smartlighting/command/mode` -> "manual"

### Mode Toggle
```bash
# Set to auto mode
mosquitto_pub -h localhost -t "smartlighting/command/mode" -m "auto"

# Set to manual mode
mosquitto_pub -h localhost -t "smartlighting/command/mode" -m "manual"
```

## Power Management

### ESP32 #1 Light Sleep

ESP32 #1 enters light sleep mode when all BLE sensors are connected and ready:

| Parameter | Value | Configurable |
|-----------|-------|--------------|
| LIGHT_SLEEP_ENABLED | True | Yes (code) |
| LIGHT_SLEEP_MS | 2000ms | Yes (code) |

**Behavior**:
- Pauses CPU while maintaining BLE connections
- Instant wake-up on BLE notifications
- Falls back to normal sleep if `machine.lightsleep()` unavailable
- Only activates when all expected sensors are connected

**Power Savings**:
- Active mode: ~80-120mA
- Light sleep: ~5-10mA
- Wake latency: <1ms on BLE interrupt

### OLED Auto-Sleep (ESP32 #2)
- Timeout: Configurable (default 15 seconds)
- Wake triggers: Button press, MQTT command
- Can be disabled via backend config
- Implementation: Hardware power off (`display.poweroff()`)

### WiFi Power Save (ESP32 #2)
- Mode: MIN_MODEM (sleep between beacons)

### BLE Optimization (ESP32 #1)
- Notification-based (no polling)
- Event-driven data transfer
- Sequential GATT operations to prevent ENOMEM
- Light sleep between notification cycles

## WiFi Provisioning

If WiFi credentials are not configured or connection fails:

1. **Enter Provisioning Mode**:
   - Hold Button A for 5 seconds during boot
   - Or automatically if WiFi fails

2. **Connect to AP**:
   - SSID: "SmartLight-Setup"
   - No password required

3. **Configure**:
   - Navigate to 192.168.4.1
   - Enter WiFi SSID and password
   - Click Save

4. **Reboot**:
   - ESP32 reboots and connects with new credentials
   - Credentials stored in NVS (Non-Volatile Storage)

## Configuration Reference

### Network Settings (config.py)
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

### LED Configuration (config.py)
```python
NUM_LEDS = 5
LED_PIN = 13
DEFAULT_BRIGHTNESS = 100
MIN_BRIGHTNESS = 0
MAX_BRIGHTNESS = 100  # Overridable by backend

LED_ROOM_NAMES = ["Living Room", "Bedroom", "Kitchen", "Bath", "Hallway"]
LED_SENSOR_MAPPING = {
    0: "SmartLight-Sensor-2",
    1: "SmartLight-Sensor-1",
}
```

### ESP32 #1 Settings (ble_central.py)
```python
# Light sleep (power saving)
LIGHT_SLEEP_ENABLED = True    # Enable light sleep mode
LIGHT_SLEEP_MS = 2000         # Sleep duration between cycles

# BLE connection
MAX_SENSORS = 2               # Expected sensor count
SCAN_TIMEOUT = 10000          # BLE scan timeout (ms)
```

### Sensor Thresholds (config.py / backend)
```python
# Temperature (color warmth)
TEMP_MIN = 20.0
TEMP_MAX = 28.0
TEMP_BLEND_STRENGTH = 0.95

# Humidity (saturation)
HUMIDITY_MIN = 30.0
HUMIDITY_MAX = 70.0
SATURATION_AT_MIN_HUMIDITY = 0.15
SATURATION_AT_MAX_HUMIDITY = 1.00

# Luminosity (auto-dim)
LUX_MIN = 0
LUX_MAX = 1200
LUX_HYSTERESIS = 10

# Audio (disco)
AUDIO_THRESHOLD = 25
AUDIO_DISCO_DURATION = 3000
AUDIO_DISCO_SPEED = 100
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
- Config request on reconnect

### Config Validation
- Invalid values are logged and ignored
- Defaults used for missing keys
- Unit conversions validated before apply

## Memory Management

Target free memory: >50KB during operation

Strategies:
- Periodic `gc.collect()`
- Compact JSON format for UART
- Stale sensor cleanup (30s timeout)
- Minimal logging in production
- RuntimeConfig caches only overrides

## Debugging

### Serial Console
Connect at 115200 baud to view logs:
```bash
# Windows
putty -serial COM3 -sercfg 115200

# Linux/macOS
screen /dev/ttyUSB0 115200
```

### Debug Logs
When `DEBUG = True` in config.py:
```
[  1s][main] LEDs ready: 5 on GPIO 13
[  2s][main] WiFi connecting to: NetworkName
[  8s][main] WiFi connected: 192.168.1.50
[ 11s][main] MQTT connected
[ 11s][main] Config request sent
[ 12s][config] Updated lighting: maxBrightness=80
[ 12s][sensor] Lux: 350, Brightness: 65 (capped at 80)
```

### Memory Status (ESP32 #2)
Heartbeat log every 10 seconds:
```
[322s][main] Up:320s Mem:50624B BLE:2/2 MQTT:OK Mode:auto
```

### ESP32 #1 Status
Heartbeat log shows power mode:
```
[120s][ble] Up:120s Sensors:2/2 Sleep:LIGHT Mem:45KB
```

Power modes:
- `LIGHT`: Light sleep active (all sensors connected)
- `NORMAL`: Normal operation (scanning or partial connection)

## Troubleshooting

### Config Not Applied
1. Check MQTT connection status
2. Verify topic subscription: `smartlighting/config/#`
3. Check serial logs for "Updated [category]" messages
4. Ensure backend is publishing to correct topics

### LEDs Not Responding to Max Brightness
1. Verify `MAX_BRIGHTNESS` is being received
2. Check if `SENSOR_OVERRIDE_ENABLED` is true
3. LEDs without sensors also apply MAX_BRIGHTNESS cap
4. Check serial logs for brightness calculations

### Mode Stuck on Manual
1. Send mode command: `smartlighting/command/mode` -> "auto"
2. Scenes no longer set mode automatically
3. Mode persists across reboots (stored in memory)

### Sensors Not Affecting Lights
1. Verify `SENSOR_OVERRIDE_ENABLED` is true
2. Check `AUTO_DIM_ENABLED` for brightness
3. Ensure mode is "auto" not "manual"
4. Verify sensor data is being received via UART
