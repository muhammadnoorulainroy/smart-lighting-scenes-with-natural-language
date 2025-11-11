-- Seed data for development
SET search_path TO smartlighting;

-- Insert test admin user (will be replaced when real OAuth login happens)
INSERT INTO users (id, email, name, role, provider, provider_sub, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@smartlighting.com',
    'Admin User',
    'ADMIN',
    'LOCAL',
    'admin-local-sub',
    true
) ON CONFLICT (email) DO NOTHING;

-- Insert sample rooms
INSERT INTO rooms (id, name, description, created_by)
VALUES 
    ('11111111-0000-0000-0000-000000000001', 'Living Room', 'Main living area with TV and seating', '00000000-0000-0000-0000-000000000001'),
    ('11111111-0000-0000-0000-000000000002', 'Bedroom', 'Master bedroom', '00000000-0000-0000-0000-000000000001'),
    ('11111111-0000-0000-0000-000000000003', 'Kitchen', 'Cooking and dining area', '00000000-0000-0000-0000-000000000001'),
    ('11111111-0000-0000-0000-000000000004', 'Bathroom', 'Main bathroom', '00000000-0000-0000-0000-000000000001')
ON CONFLICT (name) DO NOTHING;

-- Insert sample devices
INSERT INTO devices (id, room_id, type, name, mqtt_cmd_topic, mqtt_state_topic, meta_json, is_active)
VALUES
    ('22222222-0000-0000-0000-000000000001', 
     '11111111-0000-0000-0000-000000000001', 
     'LIGHT', 
     'Ceiling Light',
     'home/living_room/ceiling_light/cmd',
     'home/living_room/ceiling_light/state',
     '{"manufacturer": "Philips", "model": "Hue White", "maxBrightness": 100}',
     true),
    
    ('22222222-0000-0000-0000-000000000002', 
     '11111111-0000-0000-0000-000000000001', 
     'LIGHT', 
     'Floor Lamp',
     'home/living_room/floor_lamp/cmd',
     'home/living_room/floor_lamp/state',
     '{"manufacturer": "IKEA", "model": "TRADFRI", "maxBrightness": 100}',
     true),
    
    ('22222222-0000-0000-0000-000000000003', 
     '11111111-0000-0000-0000-000000000002', 
     'LIGHT', 
     'Bedside Lamp',
     'home/bedroom/bedside_lamp/cmd',
     'home/bedroom/bedside_lamp/state',
     '{"manufacturer": "Philips", "model": "Hue Go", "maxBrightness": 100, "supportsColor": true}',
     true),
    
    ('22222222-0000-0000-0000-000000000004', 
     '11111111-0000-0000-0000-000000000003', 
     'LIGHT', 
     'Kitchen Light',
     'home/kitchen/main_light/cmd',
     'home/kitchen/main_light/state',
     '{"manufacturer": "Generic", "model": "LED Panel", "maxBrightness": 100}',
     true)
ON CONFLICT (room_id, name) DO NOTHING;

-- Insert initial device states
INSERT INTO device_state_latest (device_id, is_on, brightness_pct, color_temp_mired)
VALUES
    ('22222222-0000-0000-0000-000000000001', false, 50, 370),
    ('22222222-0000-0000-0000-000000000002', false, 30, 450),
    ('22222222-0000-0000-0000-000000000003', false, 20, 500),
    ('22222222-0000-0000-0000-000000000004', true, 80, 300)
ON CONFLICT (device_id) DO NOTHING;

-- Insert sample scenes
INSERT INTO scenes (id, name, description, owner_id, actions_json, is_global)
VALUES
    ('33333333-0000-0000-0000-000000000001',
     'Reading',
     'Bright lighting for reading',
     '00000000-0000-0000-0000-000000000001',
     '[{"deviceId": "22222222-0000-0000-0000-000000000001", "on": true, "brightness": 90, "colorTemp": 250},
       {"deviceId": "22222222-0000-0000-0000-000000000002", "on": true, "brightness": 70, "colorTemp": 250}]',
     true),
    
    ('33333333-0000-0000-0000-000000000002',
     'Movie',
     'Dimmed lighting for watching movies',
     '00000000-0000-0000-0000-000000000001',
     '[{"deviceId": "22222222-0000-0000-0000-000000000001", "on": false},
       {"deviceId": "22222222-0000-0000-0000-000000000002", "on": true, "brightness": 15, "colorTemp": 500}]',
     true),
    
    ('33333333-0000-0000-0000-000000000003',
     'Relax',
     'Warm, comfortable lighting',
     '00000000-0000-0000-0000-000000000001',
     '[{"deviceId": "22222222-0000-0000-0000-000000000001", "on": true, "brightness": 40, "colorTemp": 450},
       {"deviceId": "22222222-0000-0000-0000-000000000002", "on": true, "brightness": 30, "colorTemp": 500},
       {"deviceId": "22222222-0000-0000-0000-000000000003", "on": true, "brightness": 20, "colorTemp": 500}]',
     true),
    
    ('33333333-0000-0000-0000-000000000004',
     'Night',
     'Minimal lighting for nighttime',
     '00000000-0000-0000-0000-000000000001',
     '[{"deviceId": "22222222-0000-0000-0000-000000000001", "on": false},
       {"deviceId": "22222222-0000-0000-0000-000000000002", "on": false},
       {"deviceId": "22222222-0000-0000-0000-000000000003", "on": true, "brightness": 5, "colorTemp": 500},
       {"deviceId": "22222222-0000-0000-0000-000000000004", "on": false}]',
     true)
ON CONFLICT (name, owner_id) DO NOTHING;
