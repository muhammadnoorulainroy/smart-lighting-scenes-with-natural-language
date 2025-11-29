"""
nRF52840 Sensor #1 - BLE Environmental Sensor
Adafruit Feather Sense: BMP280 (temp/pressure), SHT31 (humidity), APDS9960 (light), PDM Mic
"""

import time
import board
import array
import audiobusio
import adafruit_bmp280
import adafruit_sht31d
import adafruit_apds9960.apds9960
from adafruit_ble import BLERadio
from adafruit_ble.advertising.standard import ProvideServicesAdvertisement
from adafruit_ble.services import Service
from adafruit_ble.characteristics import Characteristic
from adafruit_ble.characteristics.int import Int16Characteristic, Uint16Characteristic
from adafruit_ble.uuid import StandardUUID

DEVICE_NAME = "SmartLight-Sensor-1"

TEMP_THRESHOLD = 0.5
HUMIDITY_THRESHOLD = 2.0
LUX_THRESHOLD = 10
AUDIO_THRESHOLD = 10

MEASUREMENT_INTERVAL = 2.0


class EnvironmentalSensingService(Service):
    uuid = StandardUUID(0x181A)
    
    temperature = Int16Characteristic(
        uuid=StandardUUID(0x2A6E),
        properties=Characteristic.READ | Characteristic.NOTIFY,
    )
    
    humidity = Uint16Characteristic(
        uuid=StandardUUID(0x2A6F),
        properties=Characteristic.READ | Characteristic.NOTIFY,
    )
    
    luminosity = Uint16Characteristic(
        uuid=StandardUUID(0x2A77),
        properties=Characteristic.READ | Characteristic.NOTIFY,
    )
    
    audio_level = Uint16Characteristic(
        uuid=StandardUUID(0x2A78),
        properties=Characteristic.READ | Characteristic.NOTIFY,
    )


class SensorManager:
    def __init__(self):
        self.i2c = board.I2C()
        
        # BMP280 - Temperature, Pressure
        self.bmp280 = adafruit_bmp280.Adafruit_BMP280_I2C(self.i2c)
        print("[SENSOR] BMP280 OK (temp/pressure)")
        
        # SHT31 - Humidity
        self.sht31 = adafruit_sht31d.SHT31D(self.i2c)
        print("[SENSOR] SHT31 OK (humidity)")
        
        # APDS9960 - Light, Color, Proximity
        self.apds = adafruit_apds9960.apds9960.APDS9960(self.i2c)
        self.apds.enable_color = True
        print("[SENSOR] APDS9960 OK (light)")
        
        # PDM Microphone
        self.mic = audiobusio.PDMIn(
            board.MICROPHONE_CLOCK,
            board.MICROPHONE_DATA,
            sample_rate=16000,
            bit_depth=16
        )
        self.mic_samples = array.array('H', [0] * 160)
        print("[SENSOR] PDM Mic OK")
        
        self.last_temp = None
        self.last_humidity = None
        self.last_lux = None
        self.last_audio = None
    
    def read_temperature(self):
        return self.bmp280.temperature
    
    def read_humidity(self):
        return self.sht31.relative_humidity
    
    def read_pressure(self):
        return self.bmp280.pressure
    
    def read_luminosity(self):
        r, g, b, c = self.apds.color_data
        return int(min(65535, c))
    
    def read_audio_level(self):
        self.mic.record(self.mic_samples, len(self.mic_samples))
        
        min_val = 30000
        max_val = -30000
        for sample in self.mic_samples:
            if sample < min_val:
                min_val = sample
            if sample > max_val:
                max_val = sample
        
        peak = max_val - min_val
        return int(min(100, peak / 300))
    
    def get_changed_values(self):
        changed = {}
        
        temp = self.read_temperature()
        if self.last_temp is None or abs(temp - self.last_temp) >= TEMP_THRESHOLD:
            changed['temperature'] = temp
            self.last_temp = temp
        
        humidity = self.read_humidity()
        if self.last_humidity is None or abs(humidity - self.last_humidity) >= HUMIDITY_THRESHOLD:
            changed['humidity'] = humidity
            self.last_humidity = humidity
        
        lux = self.read_luminosity()
        if self.last_lux is None or abs(lux - self.last_lux) >= LUX_THRESHOLD:
            changed['luminosity'] = lux
            self.last_lux = lux
        
        audio = self.read_audio_level()
        if self.last_audio is None or abs(audio - self.last_audio) >= AUDIO_THRESHOLD:
            changed['audio'] = audio
            self.last_audio = audio
        
        return changed


def to_ble_temperature(celsius):
    return int(celsius * 100)


def to_ble_humidity(percent):
    return int(percent * 100)


def main():
    print("\n" + "=" * 40)
    print("  {}".format(DEVICE_NAME))
    print("=" * 40 + "\n")
    
    ble = BLERadio()
    ble.name = DEVICE_NAME
    
    env_service = EnvironmentalSensingService()
    advertisement = ProvideServicesAdvertisement(env_service)
    advertisement.complete_name = DEVICE_NAME
    
    sensors = SensorManager()
    
    print("[BLE] Ready")
    
    while True:
        print("\n[BLE] Advertising...")
        ble.start_advertising(advertisement)
        
        while not ble.connected:
            time.sleep(0.5)
        
        ble.stop_advertising()
        print("[BLE] Connected!")
        
        # Send ALL sensor values immediately on connection
        temp = sensors.read_temperature()
        humidity = sensors.read_humidity()
        pressure = sensors.read_pressure()
        lux = sensors.read_luminosity()
        audio = sensors.read_audio_level()
        
        # Force notify all values
        env_service.temperature = to_ble_temperature(temp)
        time.sleep(0.1)
        env_service.humidity = to_ble_humidity(humidity)
        time.sleep(0.1)
        env_service.luminosity = lux
        time.sleep(0.1)
        env_service.audio_level = audio
        time.sleep(0.1)
        
        # Store as last values
        sensors.last_temp = temp
        sensors.last_humidity = humidity
        sensors.last_lux = lux
        sensors.last_audio = audio
        
        print("[INIT] T={:.1f}C H={:.1f}% P={:.0f}hPa L={} A={}".format(
            temp, humidity, pressure, lux, audio))
        
        while ble.connected:
            changed = sensors.get_changed_values()
            
            if changed:
                if 'temperature' in changed:
                    env_service.temperature = to_ble_temperature(changed['temperature'])
                    print("[NOTIFY] T={:.1f}C".format(changed['temperature']))
                
                if 'humidity' in changed:
                    env_service.humidity = to_ble_humidity(changed['humidity'])
                    print("[NOTIFY] H={:.1f}%".format(changed['humidity']))
                
                if 'luminosity' in changed:
                    env_service.luminosity = changed['luminosity']
                    print("[NOTIFY] L={}".format(changed['luminosity']))
                
                if 'audio' in changed:
                    env_service.audio_level = changed['audio']
                    print("[NOTIFY] A={}".format(changed['audio']))
            
            time.sleep(MEASUREMENT_INTERVAL)
        
        print("[BLE] Disconnected!")


if __name__ == "__main__":
    main()
