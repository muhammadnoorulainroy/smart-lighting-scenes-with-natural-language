-- System configuration table for storing runtime settings
-- Settings are fetched by ESP32 devices on boot and updated in real-time via MQTT

CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255),
    settings JSONB NOT NULL DEFAULT '{}',
    updated_at TIMESTAMP,
    updated_by VARCHAR(255)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_system_config_updated_at ON system_config(updated_at);

-- Insert default configurations
INSERT INTO system_config (config_key, description, settings, updated_at) VALUES
    ('lighting', 'Lighting mode and brightness settings', 
     '{"globalMode": "auto", "autoDimEnabled": true, "sensorOverrideEnabled": true, "minBrightness": 0, "maxBrightness": 100, "luxMin": 50, "luxMax": 2000}',
     NOW()),
    ('climate', 'Temperature and humidity color adjustments',
     '{"tempMin": 20, "tempMax": 28, "tempBlendStrength": 95, "humidityMin": 30, "humidityMax": 70, "saturationAtMinHumidity": 60, "saturationAtMaxHumidity": 100}',
     NOW()),
    ('audio', 'Audio detection and disco mode settings',
     '{"discoEnabled": true, "audioThreshold": 25, "discoDuration": 3000, "discoSpeed": 100, "flashBrightness": 100}',
     NOW()),
    ('display', 'OLED display settings',
     '{"oledAutoSleep": true, "oledTimeout": 15, "showSensorData": true, "showTime": true}',
     NOW()),
    ('mqtt', 'MQTT communication settings',
     '{"publishInterval": 2000, "heartbeatInterval": 10000}',
     NOW())
ON CONFLICT (config_key) DO NOTHING;

