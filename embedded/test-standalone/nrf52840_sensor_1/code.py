"""
nRF52840 Bluefruit Sense - Sensor Broadcaster #1
Reads sensors and broadcasts via BLE
Maps to LED 0 (Room 1)
"""

import time
import board
import busio
import adafruit_apds9960.apds9960
import adafruit_bmp280
import adafruit_sht31d
from adafruit_ble import BLERadio
from adafruit_ble.advertising.standard import ProvideServicesAdvertisement
from adafruit_ble.services.nordic import UARTService
import audiobusio
import array
import math

# ============================================
# CONFIGURATION
# ============================================
DEVICE_NAME = "SmartLight-Sensor-1"
SENSOR_UPDATE_INTERVAL = 0.5  # seconds (500ms)

# ============================================
# HARDWARE SETUP
# ============================================

print(f"Initializing {DEVICE_NAME}...")

# I2C for sensors
i2c = busio.I2C(board.SCL, board.SDA)

# APDS9960 - Proximity, Light, Color, Gesture
apds = adafruit_apds9960.apds9960.APDS9960(i2c)
apds.enable_proximity = True
apds.enable_color = True

# BMP280 - Temperature and Pressure
bmp280 = adafruit_bmp280.Adafruit_BMP280_I2C(i2c)
bmp280.sea_level_pressure = 1013.25

# SHT31-D - Humidity and Temperature (more accurate)
sht31d = adafruit_sht31d.SHT31D(i2c)

# PDM Microphone
mic = audiobusio.PDMIn(
    board.MICROPHONE_CLOCK,
    board.MICROPHONE_DATA,
    sample_rate=16000,
    bit_depth=16
)

# BLE Setup
ble = BLERadio()
ble.name = DEVICE_NAME
uart = UARTService()
advertisement = ProvideServicesAdvertisement(uart)

print(f"BLE Name: {DEVICE_NAME}")
print("Sensors initialized!")

# ============================================
# HELPER FUNCTIONS
# ============================================

def get_luminosity():
    """Get ambient light level in lux (approximation)"""
    try:
        r, g, b, c = apds.color_data
        # Approximate lux from clear channel
        lux = c * 0.2  # Rough conversion
        return int(lux)
    except:
        return 0

def get_temperature():
    """Get temperature in Celsius"""
    try:
        # Use SHT31D (more accurate)
        return round(sht31d.temperature, 1)
    except:
        try:
            # Fallback to BMP280
            return round(bmp280.temperature, 1)
        except:
            return 20.0

def get_humidity():
    """Get humidity percentage"""
    try:
        return round(sht31d.relative_humidity, 1)
    except:
        return 50.0

def get_audio_level():
    """Get audio peak level (0-1023)"""
    try:
        samples = array.array('H', [0] * 160)
        mic.record(samples, len(samples))
        
        # Calculate RMS
        mean = sum(samples) / len(samples)
        sum_squares = sum((s - mean) ** 2 for s in samples)
        rms = math.sqrt(sum_squares / len(samples))
        
        # Normalize to 0-1023 range
        peak = int(min(rms / 10, 1023))
        return peak
    except:
        return 0

def format_sensor_data():
    """Format sensor data as comma-separated string"""
    lux = get_luminosity()
    temp = get_temperature()
    humidity = get_humidity()
    audio = get_audio_level()
    
    # Format: LUX,TEMP,HUMIDITY,AUDIO
    return f"{lux},{temp:.1f},{humidity:.1f},{audio}"

# ============================================
# MAIN LOOP
# ============================================

print("Starting BLE advertising...")
print("Waiting for ESP32 to connect...")

while True:
    # Advertise and wait for connection
    ble.start_advertising(advertisement)
    
    while not ble.connected:
        time.sleep(0.1)
    
    print("Connected to ESP32!")
    ble.stop_advertising()
    
    # Connected - send sensor data
    while ble.connected:
        try:
            # Read all sensors
            data_str = format_sensor_data()
            
            # Send via UART
            uart.write((data_str + "\n").encode('utf-8'))
            
            # Debug print
            print(f"Sent: {data_str}")
            
            time.sleep(SENSOR_UPDATE_INTERVAL)
            
        except Exception as e:
            print(f"Error: {e}")
            break
    
    print("Disconnected. Restarting advertising...")
    time.sleep(1)


