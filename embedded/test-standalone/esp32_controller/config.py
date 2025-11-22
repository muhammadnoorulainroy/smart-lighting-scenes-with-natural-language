"""
Configuration for ESP32 Smart Lighting Controller
"""

# ============================================
# HARDWARE PINS
# ============================================
LED_PIN = 13          # GPIO 13 for WS2812B
NUM_LEDS = 5          # Total LEDs

OLED_SDA = 23         # I2C SDA (Feather default: 23)
OLED_SCL = 22         # I2C SCL (Feather default: 22)
OLED_WIDTH = 128
OLED_HEIGHT = 64
OLED_ADDR = 0x3C      # Common: 0x3C or 0x3D

# ============================================
# WIFI & MQTT CONFIGURATION (Optional)
# ============================================
WIFI_ENABLED = False   # Set to True to enable WiFi/MQTT
WIFI_SSID = "YourWiFiSSID"
WIFI_PASSWORD = "YourWiFiPassword"

MQTT_ENABLED = False   # Set to True to enable MQTT
MQTT_BROKER = "192.168.1.100"  # Backend server IP
MQTT_PORT = 1883
MQTT_CLIENT_ID = "esp32-001"
MQTT_TOPIC_PREFIX = "smartlighting"

# ============================================
# BLE SENSOR CONFIGURATION
# ============================================
SENSOR_DEVICES = {
    'sensor_1': {
        'name': 'SmartLight-Sensor-1',
        'led_index': 0,  # Maps to LED 0 (Room 1)
        'room_name': 'Living'
    },
    'sensor_2': {
        'name': 'SmartLight-Sensor-2',
        'led_index': 1,  # Maps to LED 1 (Room 2)
        'room_name': 'Bedroom'
    }
}

# Rooms without sensors (LED 2, 3, 4)
STATIC_ROOMS = [
    {'led_index': 2, 'room_name': 'Kitchen', 'color': (255, 180, 0), 'brightness': 5},    # Warm yellow/orange
    {'led_index': 3, 'room_name': 'Bath', 'color': (150, 0, 255), 'brightness': 5},       # Purple
    {'led_index': 4, 'room_name': 'Hallway', 'color': (0, 200, 255), 'brightness': 5},    # Cyan/Blue
]

# ============================================
# SENSOR EFFECT CONFIGURATION
# ============================================

# Luminosity → Brightness (inverse: high lux = dim LEDs)
LUMINOSITY_HIGH = 800    # Bright room (lux)
LUMINOSITY_LOW = 50      # Dark room (lux)
BRIGHTNESS_AT_HIGH = 3   # % LED brightness when room is bright (very dim)
BRIGHTNESS_AT_LOW = 10   # % LED brightness when room is dark (max 10%)

# Temperature → Color Temperature (linear)
TEMP_MIN = 15.0          # °C (cold)
TEMP_MAX = 30.0          # °C (hot)
COLOR_TEMP_WARM = 2700   # Kelvin (warm white)
COLOR_TEMP_COOL = 6500   # Kelvin (cool white)

# Humidity → Saturation (linear from 60% to 100%)
HUMIDITY_MIN = 60.0      # % (start saturation)
HUMIDITY_MAX = 100.0     # % (max saturation)
SATURATION_MIN = 40      # % saturation at HUMIDITY_MIN
SATURATION_MAX = 100     # % saturation at HUMIDITY_MAX

# Audio → Flash effect
AUDIO_THRESHOLD = 150    # Audio peak threshold (0-1023) - lowered for clap detection
AUDIO_FLASH_DURATION = 3000  # milliseconds (3 seconds)
AUDIO_DEBOUNCE = 500     # milliseconds (ignore rapid beats)

# ============================================
# SYSTEM CONFIGURATION
# ============================================
GLOBAL_MODE = 'auto'     # 'auto' or 'manual'
UPDATE_RATE = 50         # milliseconds (20 Hz)
BLE_SCAN_INTERVAL = 5    # seconds between BLE scans
OLED_UPDATE_RATE = 200   # milliseconds (5 Hz)

# ============================================
# DEFAULT LED COLORS (for manual/fallback)
# ============================================
DEFAULT_COLORS = [
    (255, 50, 0),     # LED 0 - Orange/Red
    (0, 255, 100),    # LED 1 - Green
    (255, 180, 100),  # LED 2 - Warm orange
    (255, 0, 150),    # LED 3 - Magenta
    (100, 150, 255),  # LED 4 - Blue
]

DEFAULT_BRIGHTNESS = 5  # % (very low for direct viewing)


