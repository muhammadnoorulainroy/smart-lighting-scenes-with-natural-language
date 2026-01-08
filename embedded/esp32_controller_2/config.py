# config.py - ESP32 #2 (Main Controller)

DEBUG = True

# Network - UPDATE THESE FOR YOUR HOME WIFI
WIFI_SSID = "YOUR_HOME_WIFI_NAME" 
WIFI_PASSWORD = "YOUR_HOME_WIFI_PASSWORD"
WIFI_TIMEOUT = 20

# MQTT - Set to your Windows machine's IP (running Docker with Mosquitto)
MQTT_BROKER = "192.168.1.30"
MQTT_PORT = 1883
MQTT_CLIENT_ID = "esp32-smartlight"
MQTT_BASE_TOPIC = "smartlighting"
MQTT_USER = None
MQTT_PASSWORD = None
MQTT_KEEPALIVE = 60

# UART (from ESP32 #1)
UART_RX_PIN = 16
UART_TX_PIN = 17
UART_BAUDRATE = 115200

# LEDs (WS2812B)
NUM_LEDS = 5
LED_PIN = 13
DEFAULT_BRIGHTNESS = 100  # Starting brightness (sensor can dim from this)
MIN_BRIGHTNESS = 0
MAX_BRIGHTNESS = 100  # Can be overridden by backend config

DEFAULT_COLORS = [
    (255, 50, 0),
    (0, 255, 100),
    (100, 100, 255),
    (255, 255, 50),
    (255, 0, 255),
]

LED_ROOM_NAMES = [
    "Kitchen",       # LED 0
    "Bedroom",       # LED 1
    "Bathroom",      # LED 2
    "Hallway",       # LED 3
    "Living Room",   # LED 4
]

# Sensor to LED mapping (only Bedroom and Living Room have sensors)
LED_SENSOR_MAPPING = {
    1: "SmartLight-Sensor-1",  # Bedroom
    4: "SmartLight-Sensor-2",  # Living Room
}

SENSOR_DEVICES = {
    "SmartLight-Sensor-1": {"led_index": 1, "room_name": "Bedroom"},
    "SmartLight-Sensor-2": {"led_index": 4, "room_name": "Living Room"},
}

# OLED Display (SH1107)
OLED_SDA = 23
OLED_SCL = 22
OLED_WIDTH = 128
OLED_HEIGHT = 64
OLED_ADDR = 0x3C
OLED_ROTATE = 180  # 0, 90, 180, or 270 degrees (180 = flip upside down)
OLED_ENABLED = True
OLED_AUTO_SLEEP = True  # Auto-sleep to save power
OLED_TIMEOUT = 15  # Seconds before auto-sleep
SHOW_TIME = True  # Display time on home page
SHOW_SENSOR_DATA = True  # Display sensor values on detail pages
OLED_PAGE_HOME = 0
OLED_PAGE_BRIGHTNESS = 1
OLED_PAGE_SENSOR_START = 2

# Buttons
BUTTON_OLED_A = 15
BUTTON_OLED_B = 32
BUTTON_OLED_C = 14
BUTTON_DEBOUNCE_MS = 200
FACTORY_RESET_BUTTON = 15
FACTORY_RESET_HOLD_SECONDS = 5

# Sensor Effects - Temperature (color temperature)
TEMP_EFFECTS_ENABLED = True
TEMP_MIN = 20.0
TEMP_MAX = 28.0
TEMP_BLEND_STRENGTH = 0.95
COLOR_GRADIENT_COLD = (50, 100, 255)
COLOR_GRADIENT_NEUTRAL = (255, 255, 200)
COLOR_GRADIENT_HOT = (255, 80, 0)

# Sensor Effects - Humidity (saturation)
HUMIDITY_EFFECTS_ENABLED = True
HUMIDITY_MIN = 30.0
HUMIDITY_MAX = 70.0
SATURATION_AT_MIN_HUMIDITY = 0.15
SATURATION_AT_MAX_HUMIDITY = 1.00

# Sensor Effects - Luminosity (auto-dim)
AUTO_DIM_ENABLED = True
LUX_MIN = 0
LUX_MAX = 1200
LUX_HYSTERESIS = 10

# Sensor Effects - Audio (disco mode)
AUDIO_THRESHOLD = 25
AUDIO_DISCO_DURATION = 3000
AUDIO_DISCO_SPEED = 100
AUDIO_FLASH_BRIGHTNESS = 100

def get_flash_color(led_index):
    colors = [
        (255, 255, 255),
        (0, 255, 255),
        (255, 0, 255),
        (255, 255, 0),
        (0, 255, 0),
    ]
    return colors[led_index % len(colors)]
