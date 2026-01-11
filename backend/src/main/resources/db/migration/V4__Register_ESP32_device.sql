-- V4__Register_ESP32_device.sql
-- Register ESP32 controller and its LED devices

SET search_path TO smartlighting;

-- Insert default room (home)
INSERT INTO rooms (name, description) VALUES
('Home', 'Default home with 5 LED zones')
ON CONFLICT DO NOTHING;

-- Get the home room ID (we'll use it multiple times)
DO $$
DECLARE
    v_home_room_id UUID;
    v_controller_id UUID;
    v_led0_mapping_id UUID;
    v_led1_mapping_id UUID;
    v_led2_mapping_id UUID;
    v_led3_mapping_id UUID;
    v_led4_mapping_id UUID;
BEGIN
    -- Get home room ID
    SELECT id INTO v_home_room_id FROM rooms WHERE name = 'Home' LIMIT 1;

    -- Insert ESP32 controller
    INSERT INTO esp32_controllers (
        device_id,
        name,
        num_leds,
        mqtt_base_topic,
        ip_address,
        status
    ) VALUES (
        'esp32-001',
        'ESP32-Main-Controller',
        5,
        'smartlighting',
        '192.168.1.100',  -- Will be updated dynamically
        'offline'
    )
    ON CONFLICT (device_id) DO UPDATE SET
        name = EXCLUDED.name,
        num_leds = EXCLUDED.num_leds,
        mqtt_base_topic = EXCLUDED.mqtt_base_topic
    RETURNING id INTO v_controller_id;

    -- If controller already existed, get its ID
    IF v_controller_id IS NULL THEN
        SELECT id INTO v_controller_id FROM esp32_controllers WHERE device_id = 'esp32-001';
    END IF;

    -- Create LED mappings for each LED
    -- LED 0 - Living Room
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 0, v_home_room_id)
    ON CONFLICT (controller_id, led_index) DO NOTHING
    RETURNING id INTO v_led0_mapping_id;

    IF v_led0_mapping_id IS NULL THEN
        SELECT id INTO v_led0_mapping_id FROM led_mappings
        WHERE controller_id = v_controller_id AND led_index = 0;
    END IF;

    -- LED 1 - Bedroom
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 1, v_home_room_id)
    ON CONFLICT (controller_id, led_index) DO NOTHING
    RETURNING id INTO v_led1_mapping_id;

    IF v_led1_mapping_id IS NULL THEN
        SELECT id INTO v_led1_mapping_id FROM led_mappings
        WHERE controller_id = v_controller_id AND led_index = 1;
    END IF;

    -- LED 2 - Kitchen
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 2, v_home_room_id)
    ON CONFLICT (controller_id, led_index) DO NOTHING
    RETURNING id INTO v_led2_mapping_id;

    IF v_led2_mapping_id IS NULL THEN
        SELECT id INTO v_led2_mapping_id FROM led_mappings
        WHERE controller_id = v_controller_id AND led_index = 2;
    END IF;

    -- LED 3 - Bathroom
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 3, v_home_room_id)
    ON CONFLICT (controller_id, led_index) DO NOTHING
    RETURNING id INTO v_led3_mapping_id;

    IF v_led3_mapping_id IS NULL THEN
        SELECT id INTO v_led3_mapping_id FROM led_mappings
        WHERE controller_id = v_controller_id AND led_index = 3;
    END IF;

    -- LED 4 - Hallway
    INSERT INTO led_mappings (controller_id, led_index, room_id)
    VALUES (v_controller_id, 4, v_home_room_id)
    ON CONFLICT (controller_id, led_index) DO NOTHING
    RETURNING id INTO v_led4_mapping_id;

    IF v_led4_mapping_id IS NULL THEN
        SELECT id INTO v_led4_mapping_id FROM led_mappings
        WHERE controller_id = v_controller_id AND led_index = 4;
    END IF;

    -- Insert 5 LED devices (one for each LED strip position)
    -- LED 0 - Living Room Light
    IF NOT EXISTS (SELECT 1 FROM devices WHERE led_mapping_id = v_led0_mapping_id) THEN
        INSERT INTO devices (
            name,
            type,
            room_id,
            controller_id,
            led_mapping_id,
            mqtt_cmd_topic,
            mqtt_state_topic,
            meta_json,
            is_active
        ) VALUES (
            'Living Room Light',
            'LIGHT',
            v_home_room_id,
            v_controller_id,
            v_led0_mapping_id,
            'smartlighting/command/esp32-001/led/0',
            'smartlighting/status/esp32-001',
            '{"led_index": 0, "rgb": true, "brightness": true, "sensor_control": true}'::jsonb,
            true
        );
    END IF;

    -- LED 1 - Bedroom Light
    IF NOT EXISTS (SELECT 1 FROM devices WHERE led_mapping_id = v_led1_mapping_id) THEN
        INSERT INTO devices (
            name,
            type,
            room_id,
            controller_id,
            led_mapping_id,
            mqtt_cmd_topic,
            mqtt_state_topic,
            meta_json,
            is_active
        ) VALUES (
            'Bedroom Light',
            'LIGHT',
            v_home_room_id,
            v_controller_id,
            v_led1_mapping_id,
            'smartlighting/command/esp32-001/led/1',
            'smartlighting/status/esp32-001',
            '{"led_index": 1, "rgb": true, "brightness": true, "sensor_control": true}'::jsonb,
            true
        );
    END IF;

    -- LED 2 - Kitchen Light
    IF NOT EXISTS (SELECT 1 FROM devices WHERE led_mapping_id = v_led2_mapping_id) THEN
        INSERT INTO devices (
            name,
            type,
            room_id,
            controller_id,
            led_mapping_id,
            mqtt_cmd_topic,
            mqtt_state_topic,
            meta_json,
            is_active
        ) VALUES (
            'Kitchen Light',
            'LIGHT',
            v_home_room_id,
            v_controller_id,
            v_led2_mapping_id,
            'smartlighting/command/esp32-001/led/2',
            'smartlighting/status/esp32-001',
            '{"led_index": 2, "rgb": true, "brightness": true, "sensor_control": false}'::jsonb,
            true
        );
    END IF;

    -- LED 3 - Bathroom Light
    IF NOT EXISTS (SELECT 1 FROM devices WHERE led_mapping_id = v_led3_mapping_id) THEN
        INSERT INTO devices (
            name,
            type,
            room_id,
            controller_id,
            led_mapping_id,
            mqtt_cmd_topic,
            mqtt_state_topic,
            meta_json,
            is_active
        ) VALUES (
            'Bathroom Light',
            'LIGHT',
            v_home_room_id,
            v_controller_id,
            v_led3_mapping_id,
            'smartlighting/command/esp32-001/led/3',
            'smartlighting/status/esp32-001',
            '{"led_index": 3, "rgb": true, "brightness": true, "sensor_control": false}'::jsonb,
            true
        );
    END IF;

    -- LED 4 - Hallway Light
    IF NOT EXISTS (SELECT 1 FROM devices WHERE led_mapping_id = v_led4_mapping_id) THEN
        INSERT INTO devices (
            name,
            type,
            room_id,
            controller_id,
            led_mapping_id,
            mqtt_cmd_topic,
            mqtt_state_topic,
            meta_json,
            is_active
        ) VALUES (
            'Hallway Light',
            'LIGHT',
            v_home_room_id,
            v_controller_id,
            v_led4_mapping_id,
            'smartlighting/command/esp32-001/led/4',
            'smartlighting/status/esp32-001',
            '{"led_index": 4, "rgb": true, "brightness": true, "sensor_control": false}'::jsonb,
            true
        );
    END IF;

    -- Insert initial device states for all LEDs
    INSERT INTO device_state_latest (device_id, is_on, brightness_pct, rgb_color, last_seen, updated_at)
    SELECT
        d.id,
        true,
        5,  -- Default 5% brightness
        CASE (d.meta_json->>'led_index')::int
            WHEN 0 THEN '#FF3200'  -- Orange/Red
            WHEN 1 THEN '#00FF64'  -- Green
            WHEN 2 THEN '#FFB464'  -- Warm orange
            WHEN 3 THEN '#FF0096'  -- Magenta
            WHEN 4 THEN '#6496FF'  -- Blue
            ELSE '#FFFFFF'
        END,
        NOW(),
        NOW()
    FROM devices d
    WHERE d.controller_id = v_controller_id
        AND d.type = 'LIGHT'
        AND NOT EXISTS (SELECT 1 FROM device_state_latest WHERE device_id = d.id)
    ON CONFLICT (device_id) DO NOTHING;

END $$;
