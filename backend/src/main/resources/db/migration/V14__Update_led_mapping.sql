-- V14__Update_led_mapping.sql
-- Update LED to room mapping:
-- LED 0: Kitchen
-- LED 1: Bedroom (Sensor-1)
-- LED 2: Bathroom
-- LED 3: Hallway
-- LED 4: Living Room (Sensor-2)

SET search_path TO smartlighting;

DO $$
DECLARE
    v_controller_id UUID;
    v_kitchen_id UUID;
    v_bedroom_id UUID;
    v_bathroom_id UUID;
    v_hallway_id UUID;
    v_living_room_id UUID;
BEGIN
    -- Get controller ID
    SELECT id INTO v_controller_id FROM esp32_controllers WHERE device_id = 'esp32-001' LIMIT 1;
    
    IF v_controller_id IS NULL THEN
        RAISE NOTICE 'No controller found, skipping LED mapping update';
        RETURN;
    END IF;

    -- Get room IDs
    SELECT id INTO v_kitchen_id FROM rooms WHERE name = 'kitchen';
    SELECT id INTO v_bedroom_id FROM rooms WHERE name = 'bedroom';
    SELECT id INTO v_bathroom_id FROM rooms WHERE name = 'bathroom';
    SELECT id INTO v_hallway_id FROM rooms WHERE name = 'hallway';
    SELECT id INTO v_living_room_id FROM rooms WHERE name = 'living-room';

    -- Update LED mappings
    UPDATE led_mappings SET room_id = v_kitchen_id WHERE controller_id = v_controller_id AND led_index = 0;
    UPDATE led_mappings SET room_id = v_bedroom_id WHERE controller_id = v_controller_id AND led_index = 1;
    UPDATE led_mappings SET room_id = v_bathroom_id WHERE controller_id = v_controller_id AND led_index = 2;
    UPDATE led_mappings SET room_id = v_hallway_id WHERE controller_id = v_controller_id AND led_index = 3;
    UPDATE led_mappings SET room_id = v_living_room_id WHERE controller_id = v_controller_id AND led_index = 4;

    -- Update devices: set led_index based on their room's NEW led_mapping
    UPDATE devices d
    SET 
        meta_json = jsonb_set(d.meta_json, '{led_index}', to_jsonb(lm.led_index)),
        led_mapping_id = lm.id,
        mqtt_cmd_topic = 'smartlighting/led/' || lm.led_index || '/set',
        mqtt_state_topic = 'smartlighting/led/' || lm.led_index || '/state'
    FROM led_mappings lm
    WHERE lm.room_id = d.room_id
      AND lm.controller_id = v_controller_id
      AND d.type = 'LIGHT';

    -- Update sensor devices to point to correct rooms
    -- Sensor-1 -> Bedroom
    UPDATE devices 
    SET room_id = v_bedroom_id
    WHERE meta_json->>'sensor_id' = 'SmartLight-Sensor-1';

    -- Sensor-2 -> Living Room
    UPDATE devices 
    SET room_id = v_living_room_id
    WHERE meta_json->>'sensor_id' = 'SmartLight-Sensor-2';

    RAISE NOTICE 'LED mapping updated successfully';
END $$;

