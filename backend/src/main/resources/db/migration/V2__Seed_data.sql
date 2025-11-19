-- V2__seed_data.sql
-- Seed data for development and testing
-- Safe to run in production (creates minimal demo data)

SET search_path TO smartlighting;

-- ============================================================================
-- SEED DATA - ROOMS
-- ============================================================================

INSERT INTO rooms (id, name, description) VALUES
(1, 'bedroom', 'Master bedroom'),
(2, 'living-room', 'Main living room'),
(3, 'kitchen', 'Kitchen and dining area'),
(4, 'bathroom', 'Main bathroom'),
(5, 'hallway', 'Entrance hallway')
ON CONFLICT (name) DO NOTHING;

-- Reset sequence
SELECT setval('rooms_id_seq', (SELECT MAX(id) FROM rooms));

-- ============================================================================
-- SEED DATA - ESP32 CONTROLLER (Example)
-- ============================================================================

-- Insert example controller (will be created dynamically in production)
INSERT INTO esp32_controllers (id, device_id, name, num_leds, status, mqtt_base_topic, firmware_version) VALUES
(1, 'esp32-01', 'Main Floor Controller', 5, 'offline', 'home/devices/esp32-01', '1.0.0')
ON CONFLICT (device_id) DO NOTHING;

-- Reset sequence
SELECT setval('esp32_controllers_id_seq', (SELECT MAX(id) FROM esp32_controllers));

-- ============================================================================
-- COMPLETION
-- ============================================================================

DO $$ 
BEGIN 
    RAISE NOTICE 'Seed data V2 created successfully';
    RAISE NOTICE 'Created % rooms', (SELECT COUNT(*) FROM rooms);
    RAISE NOTICE 'Created % ESP32 controllers', (SELECT COUNT(*) FROM esp32_controllers);
END $$;
