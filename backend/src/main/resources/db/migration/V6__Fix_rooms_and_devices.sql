-- V6__Fix_rooms_and_devices.sql
-- Remove Home room and set up proper device associations

SET search_path TO smartlighting;

-- Delete the Home room (cascade will handle related records)
DELETE FROM rooms WHERE name = 'Home';

-- Mark default rooms as non-deletable
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT false;

UPDATE rooms SET is_default = true WHERE name IN ('bedroom', 'living-room', 'kitchen', 'bathroom', 'hallway');

-- Get room IDs
DO $$
DECLARE
    v_bedroom_id UUID;
    v_living_room_id UUID;
    v_kitchen_id UUID;
    v_bathroom_id UUID;
    v_hallway_id UUID;
    v_controller_id UUID;
BEGIN
    -- Get room IDs
    SELECT id INTO v_bedroom_id FROM rooms WHERE name = 'bedroom';
    SELECT id INTO v_living_room_id FROM rooms WHERE name = 'living-room';
    SELECT id INTO v_kitchen_id FROM rooms WHERE name = 'kitchen';
    SELECT id INTO v_bathroom_id FROM rooms WHERE name = 'bathroom';
    SELECT id INTO v_hallway_id FROM rooms WHERE name = 'hallway';

    -- Get or create the ESP32 controller
    SELECT id INTO v_controller_id FROM esp32_controllers WHERE device_id = 'esp32-001' LIMIT 1;
    
    IF v_controller_id IS NULL THEN
        INSERT INTO esp32_controllers (device_id, name, num_leds, mqtt_base_topic, status)
        VALUES ('esp32-001', 'ESP32-Main-Controller', 5, 'smartlighting', 'offline')
        RETURNING id INTO v_controller_id;
    END IF;

    -- Clear existing LED mappings and devices for this controller
    DELETE FROM device_state_latest WHERE device_id IN (SELECT id FROM devices WHERE controller_id = v_controller_id);
    DELETE FROM devices WHERE controller_id = v_controller_id;
    DELETE FROM led_mappings WHERE controller_id = v_controller_id;

    -- Create LED mappings for each room (LED 0-4)
    -- LED 0 - Living Room
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 0, v_living_room_id);

    -- LED 1 - Bedroom
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 1, v_bedroom_id);

    -- LED 2 - Kitchen
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 2, v_kitchen_id);

    -- LED 3 - Bathroom
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 3, v_bathroom_id);

    -- LED 4 - Hallway
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 4, v_hallway_id);

    -- Create RGB LED devices for all rooms
    INSERT INTO devices (name, type, room_id, controller_id, led_mapping_id, mqtt_cmd_topic, mqtt_state_topic, meta_json, is_active)
    SELECT 
        r.name || ' RGB LED',
        'LIGHT',
        r.id,
        v_controller_id,
        lm.id,
        'smartlighting/led/' || lm.led_index || '/set',
        'smartlighting/led/' || lm.led_index || '/state',
        json_build_object('led_index', lm.led_index, 'rgb', true, 'brightness', true)::jsonb,
        true
    FROM rooms r
    JOIN led_mappings lm ON lm.room_id = r.id AND lm.controller_id = v_controller_id
    WHERE r.name IN ('bedroom', 'living-room', 'kitchen', 'bathroom', 'hallway');

    -- Create Adafruit Feather nRF52840 Sense sensors for bedroom and living room
    INSERT INTO devices (name, type, room_id, mqtt_cmd_topic, mqtt_state_topic, meta_json, is_active)
    VALUES 
        ('Bedroom Sensor', 'SENSOR', v_bedroom_id, NULL, 'smartlighting/sensor/1', 
         '{"sensor_id": "SmartLight-Sensor-1", "capabilities": ["temperature", "humidity", "light", "pressure", "audio"]}'::jsonb, true),
        ('Living Room Sensor', 'SENSOR', v_living_room_id, NULL, 'smartlighting/sensor/2',
         '{"sensor_id": "SmartLight-Sensor-2", "capabilities": ["temperature", "humidity", "light", "pressure", "audio"]}'::jsonb, true);

    -- Initialize device states for LEDs
    INSERT INTO device_state_latest (device_id, is_on, brightness_pct, rgb_color, last_seen, updated_at)
    SELECT 
        d.id,
        true,
        50,
        CASE (d.meta_json->>'led_index')::int
            WHEN 0 THEN '#FF3200'
            WHEN 1 THEN '#00FF64'
            WHEN 2 THEN '#6496FF'
            WHEN 3 THEN '#FF0096'
            WHEN 4 THEN '#FFFF32'
            ELSE '#FFFFFF'
        END,
        NOW(),
        NOW()
    FROM devices d
    WHERE d.type = 'LIGHT' AND d.controller_id = v_controller_id;

    RAISE NOTICE 'Rooms and devices configured successfully';
END $$;

