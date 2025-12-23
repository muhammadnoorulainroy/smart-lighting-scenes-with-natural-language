-- V8__Update_adafruit_sensors.sql
-- Update Adafruit Feather nRF52840 Sense devices with full sensor capabilities
-- Reference: https://learn.adafruit.com/adafruit-feather-sense/overview

SET search_path TO smartlighting;

-- Update existing sensors to MULTI_SENSOR type with complete capabilities
-- Adafruit Feather nRF52840 Sense has:
-- - BMP280: Temperature & Barometric Pressure/Altitude
-- - SHT31: Humidity  
-- - APDS9960: Proximity, Light, Color (RGB), Gesture
-- - LSM6DS33/LSM6DS3TR-C: Accelerometer & Gyroscope (6-DoF)
-- - LIS3MDL: Magnetometer
-- - PDM Microphone: Sound/Audio level

UPDATE devices 
SET 
    type = 'MULTI_SENSOR'::device_type,
    meta_json = jsonb_build_object(
        'sensor_id', 'SmartLight-Sensor-1',
        'board', 'Adafruit Feather nRF52840 Sense',
        'sensors', jsonb_build_array(
            jsonb_build_object('id', 'temperature', 'chip', 'BMP280', 'unit', '°C'),
            jsonb_build_object('id', 'pressure', 'chip', 'BMP280', 'unit', 'hPa'),
            jsonb_build_object('id', 'humidity', 'chip', 'SHT31', 'unit', '%'),
            jsonb_build_object('id', 'light', 'chip', 'APDS9960', 'unit', 'lux'),
            jsonb_build_object('id', 'proximity', 'chip', 'APDS9960', 'unit', 'units'),
            jsonb_build_object('id', 'color', 'chip', 'APDS9960', 'unit', 'RGB'),
            jsonb_build_object('id', 'gesture', 'chip', 'APDS9960', 'unit', 'gesture'),
            jsonb_build_object('id', 'accelerometer', 'chip', 'LSM6DS3TR-C', 'unit', 'm/s²'),
            jsonb_build_object('id', 'gyroscope', 'chip', 'LSM6DS3TR-C', 'unit', '°/s'),
            jsonb_build_object('id', 'magnetometer', 'chip', 'LIS3MDL', 'unit', 'µT'),
            jsonb_build_object('id', 'audio', 'chip', 'PDM Microphone', 'unit', 'dB')
        ),
        'capabilities', jsonb_build_array(
            'temperature', 'pressure', 'humidity', 'light', 'proximity', 
            'color', 'gesture', 'accelerometer', 'gyroscope', 'magnetometer', 'audio'
        )
    ),
    name = 'Bedroom Feather Sense'
WHERE meta_json->>'sensor_id' = 'SmartLight-Sensor-1';

UPDATE devices 
SET 
    type = 'MULTI_SENSOR'::device_type,
    meta_json = jsonb_build_object(
        'sensor_id', 'SmartLight-Sensor-2',
        'board', 'Adafruit Feather nRF52840 Sense',
        'sensors', jsonb_build_array(
            jsonb_build_object('id', 'temperature', 'chip', 'BMP280', 'unit', '°C'),
            jsonb_build_object('id', 'pressure', 'chip', 'BMP280', 'unit', 'hPa'),
            jsonb_build_object('id', 'humidity', 'chip', 'SHT31', 'unit', '%'),
            jsonb_build_object('id', 'light', 'chip', 'APDS9960', 'unit', 'lux'),
            jsonb_build_object('id', 'proximity', 'chip', 'APDS9960', 'unit', 'units'),
            jsonb_build_object('id', 'color', 'chip', 'APDS9960', 'unit', 'RGB'),
            jsonb_build_object('id', 'gesture', 'chip', 'APDS9960', 'unit', 'gesture'),
            jsonb_build_object('id', 'accelerometer', 'chip', 'LSM6DS3TR-C', 'unit', 'm/s²'),
            jsonb_build_object('id', 'gyroscope', 'chip', 'LSM6DS3TR-C', 'unit', '°/s'),
            jsonb_build_object('id', 'magnetometer', 'chip', 'LIS3MDL', 'unit', 'µT'),
            jsonb_build_object('id', 'audio', 'chip', 'PDM Microphone', 'unit', 'dB')
        ),
        'capabilities', jsonb_build_array(
            'temperature', 'pressure', 'humidity', 'light', 'proximity', 
            'color', 'gesture', 'accelerometer', 'gyroscope', 'magnetometer', 'audio'
        )
    ),
    name = 'Living Room Feather Sense'
WHERE meta_json->>'sensor_id' = 'SmartLight-Sensor-2';

-- Log the update
DO $$
BEGIN
    RAISE NOTICE 'Updated Adafruit Feather nRF52840 Sense devices with full sensor capabilities';
    RAISE NOTICE 'Sensors: Temperature (BMP280), Pressure (BMP280), Humidity (SHT31)';
    RAISE NOTICE 'Sensors: Light/Proximity/Color/Gesture (APDS9960)';
    RAISE NOTICE 'Sensors: Accelerometer/Gyroscope (LSM6DS3TR-C), Magnetometer (LIS3MDL)';
    RAISE NOTICE 'Sensors: Audio (PDM Microphone)';
END $$;

