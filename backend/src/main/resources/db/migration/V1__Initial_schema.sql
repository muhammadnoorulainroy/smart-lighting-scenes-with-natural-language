-- V1__initial_schema.sql
-- Initial database schema for Smart Lighting system
-- Single home, three roles (OWNER, MEMBER, GUEST)

-- Create schema
CREATE SCHEMA IF NOT EXISTS smartlighting;

SET search_path TO smartlighting;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUMS
-- ============================================================================

-- User roles
CREATE TYPE user_role AS ENUM ('ADMIN', 'RESIDENT', 'GUEST');

-- OAuth providers
CREATE TYPE oauth_provider AS ENUM ('GOOGLE', 'LOCAL');

-- Device types
CREATE TYPE device_type AS ENUM ('LIGHT', 'SENSOR', 'SWITCH');

-- Event types
CREATE TYPE event_type AS ENUM (
    'device_state_changed',
    'scene_applied',
    'rule_fired',
    'rule_created',
    'rule_updated',
    'rule_deleted',
    'device_added',
    'device_removed',
    'user_login',
    'user_action',
    'conflict_detected',
    'system_event'
);

-- Controller status
CREATE TYPE controller_status AS ENUM ('online', 'offline', 'error');

-- ============================================================================
-- USERS & AUTHENTICATION
-- ============================================================================

CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    picture_url VARCHAR(512),
    role VARCHAR(50) NOT NULL DEFAULT 'GUEST',
    
    -- OAuth fields
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_sub VARCHAR(255),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_sub ON users(provider, provider_sub);
CREATE INDEX idx_users_role ON users(role);

COMMENT ON TABLE users IS 'System users with role-based access control';
COMMENT ON COLUMN users.role IS 'ADMIN: full control, RESIDENT: daily usage, GUEST: limited access';

-- Fix the comment to reflect actual schema
UPDATE pg_description SET description = 'Roles: ADMIN, RESIDENT, GUEST' 
WHERE objoid = 'smartlighting.flyway_schema_history'::regclass;

-- ============================================================================
-- USER INVITATIONS
-- ============================================================================

CREATE TABLE user_invitations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    invited_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    
    CONSTRAINT valid_invitation_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_invitations_token ON user_invitations(token);
CREATE INDEX idx_invitations_email ON user_invitations(email);
CREATE INDEX idx_invitations_expires_at ON user_invitations(expires_at);

COMMENT ON TABLE user_invitations IS 'Pending user invitations';

-- ============================================================================
-- ROOMS
-- ============================================================================

CREATE TABLE rooms (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Metadata
    meta_json JSONB DEFAULT '{}'::JSONB,
    
    CONSTRAINT room_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_rooms_name ON rooms(name);

COMMENT ON TABLE rooms IS 'Physical rooms in the home';

-- ============================================================================
-- ESP32 CONTROLLERS
-- ============================================================================

CREATE TABLE esp32_controllers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    num_leds INTEGER NOT NULL CHECK (num_leds > 0 AND num_leds <= 1000),
    
    -- MQTT configuration
    mqtt_base_topic VARCHAR(255),
    
    -- Status
    status controller_status DEFAULT 'offline',
    ip_address VARCHAR(50),
    firmware_version VARCHAR(50),
    last_seen TIMESTAMP,
    
    -- Management
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Metadata
    meta_json JSONB DEFAULT '{}'::JSONB,
    
    CONSTRAINT device_id_not_empty CHECK (LENGTH(TRIM(device_id)) > 0),
    CONSTRAINT valid_ip_address CHECK (
        ip_address IS NULL OR 
        ip_address ~* '^(\d{1,3}\.){3}\d{1,3}$'
    )
);

CREATE INDEX idx_controllers_device_id ON esp32_controllers(device_id);
CREATE INDEX idx_controllers_status ON esp32_controllers(status);
CREATE INDEX idx_controllers_last_seen ON esp32_controllers(last_seen);

COMMENT ON TABLE esp32_controllers IS 'ESP32 hardware controllers managing WS2812B LEDs';
COMMENT ON COLUMN esp32_controllers.num_leds IS 'Total number of LEDs connected to this controller';

-- ============================================================================
-- LED MAPPINGS
-- ============================================================================

CREATE TABLE led_mappings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    controller_id UUID NOT NULL REFERENCES esp32_controllers(id) ON DELETE CASCADE,
    led_index INTEGER NOT NULL CHECK (led_index >= 0),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    
    -- Management
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_controller_led UNIQUE(controller_id, led_index)
);

CREATE INDEX idx_led_mappings_controller ON led_mappings(controller_id);
CREATE INDEX idx_led_mappings_room ON led_mappings(room_id);
CREATE INDEX idx_led_mappings_led_index ON led_mappings(controller_id, led_index);

COMMENT ON TABLE led_mappings IS 'Maps LED indices on controllers to physical rooms';
COMMENT ON COLUMN led_mappings.led_index IS 'Zero-based index of LED on the controller (0, 1, 2, ...)';

-- ============================================================================
-- DEVICES
-- ============================================================================

CREATE TABLE devices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    controller_id UUID REFERENCES esp32_controllers(id) ON DELETE SET NULL,
    led_mapping_id UUID REFERENCES led_mappings(id) ON DELETE SET NULL,
    
    type device_type NOT NULL,
    name VARCHAR(100) NOT NULL,
    
    -- MQTT topics
    mqtt_cmd_topic VARCHAR(255),
    mqtt_state_topic VARCHAR(255),
    mqtt_tele_topic VARCHAR(255),
    mqtt_status_topic VARCHAR(255),
    
    -- Management
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Capabilities and metadata
    meta_json JSONB DEFAULT '{}'::JSONB,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    CONSTRAINT device_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_devices_room ON devices(room_id);
CREATE INDEX idx_devices_type ON devices(type);
CREATE INDEX idx_devices_controller ON devices(controller_id);
CREATE INDEX idx_devices_led_mapping ON devices(led_mapping_id);

COMMENT ON TABLE devices IS 'Controllable devices (lights, sensors, switches)';
COMMENT ON COLUMN devices.meta_json IS 'Device capabilities: led_index, capabilities array, etc.';

-- ============================================================================
-- DEVICE STATE
-- ============================================================================

CREATE TABLE device_state_latest (
    device_id UUID PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
    
    -- Common state
    is_on BOOLEAN DEFAULT FALSE,
    brightness_pct INTEGER CHECK (brightness_pct >= 0 AND brightness_pct <= 100),
    
    -- Color
    rgb_color VARCHAR(7),  -- Hex color like #FF5733
    color_temp_mired INTEGER,
    
    -- Timestamps
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_state_last_seen ON device_state_latest(last_seen);

COMMENT ON TABLE device_state_latest IS 'Latest known state of each device';
COMMENT ON COLUMN device_state_latest.rgb_color IS 'Hex RGB color like #FF5733';

-- ============================================================================
-- SENSOR READINGS (Historical)
-- ============================================================================

CREATE TABLE sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Sensor data
    metric VARCHAR(50) NOT NULL,  -- 'lux', 'temperature', 'humidity', 'motion'
    value NUMERIC(10, 2) NOT NULL,
    unit VARCHAR(20),             -- 'lux', '°C', '%', 'boolean'
    
    -- Additional data
    extra_data JSONB DEFAULT '{}'::JSONB
);

CREATE INDEX idx_sensor_readings_device ON sensor_readings(device_id);
CREATE INDEX idx_sensor_readings_timestamp ON sensor_readings(timestamp DESC);
CREATE INDEX idx_sensor_readings_metric ON sensor_readings(metric);
CREATE INDEX idx_sensor_readings_device_timestamp ON sensor_readings(device_id, timestamp DESC);

COMMENT ON TABLE sensor_readings IS 'Historical sensor data for analytics';

-- ============================================================================
-- SCENES
-- ============================================================================

CREATE TABLE scenes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Ownership
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Scene definition
    actions_json JSONB NOT NULL,  -- Array of {device_id, on, brightness, rgb, ...}
    
    -- Global flag
    is_global BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT scene_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT valid_actions_json CHECK (jsonb_typeof(actions_json) = 'array')
);

CREATE INDEX idx_scenes_owner_id ON scenes(owner_id);
CREATE INDEX idx_scenes_name ON scenes(name);

COMMENT ON TABLE scenes IS 'Predefined lighting scenes (e.g., Reading, Movie Night)';
COMMENT ON COLUMN scenes.actions_json IS 'Array of device actions to apply';

-- ============================================================================
-- RULES
-- ============================================================================

CREATE TABLE rules (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Ownership
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Rule definition (DSL)
    json_dsl JSONB NOT NULL,  -- {triggers: [], conditions: [], actions: []}
    
    -- Priority and status
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_fired_at TIMESTAMP,
    
    CONSTRAINT rule_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT valid_rule_dsl CHECK (
        jsonb_typeof(json_dsl) = 'object' AND
        json_dsl ? 'triggers' AND
        json_dsl ? 'actions'
    )
);

CREATE INDEX idx_rules_created_by ON rules(created_by);
CREATE INDEX idx_rules_enabled ON rules(enabled);
CREATE INDEX idx_rules_priority ON rules(priority DESC);

COMMENT ON TABLE rules IS 'Automation rules created from natural language';
COMMENT ON COLUMN rules.json_dsl IS 'Rule definition: triggers, conditions, actions';
COMMENT ON COLUMN rules.priority IS 'Higher priority rules win in conflicts';

-- ============================================================================
-- SCHEDULES
-- ============================================================================

CREATE TABLE schedules (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    
    -- Schedule specification
    compiled_spec_json JSONB NOT NULL,  -- {cron: '...', timezone: '...', sunset_offset: ...}
    
    -- Next execution
    next_fire_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_schedule_spec CHECK (jsonb_typeof(compiled_spec_json) = 'object')
);

CREATE INDEX idx_schedules_rule ON schedules(rule_id);
CREATE INDEX idx_schedules_next_fire ON schedules(next_fire_at) WHERE next_fire_at IS NOT NULL;

COMMENT ON TABLE schedules IS 'Compiled schedule information for rules';
COMMENT ON COLUMN schedules.compiled_spec_json IS 'Cron expressions, timezone, sunrise/sunset offsets';

-- ============================================================================
-- EVENTS & LOGS
-- ============================================================================

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Event classification
    type event_type NOT NULL,
    
    -- Related entities
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    rule_id UUID REFERENCES rules(id) ON DELETE SET NULL,
    scene_id UUID REFERENCES scenes(id) ON DELETE SET NULL,
    
    -- Event details
    details_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    
    -- For "Why?" explanations
    cause_chain JSONB,  -- Array tracking causation: [rule_fired, scene_applied, device_changed]
    
    CONSTRAINT valid_details_json CHECK (jsonb_typeof(details_json) = 'object')
);

CREATE INDEX idx_events_timestamp ON events(timestamp DESC);
CREATE INDEX idx_events_type ON events(type);
CREATE INDEX idx_events_actor_user ON events(actor_user_id);
CREATE INDEX idx_events_device ON events(device_id);
CREATE INDEX idx_events_rule ON events(rule_id);
CREATE INDEX idx_events_scene ON events(scene_id);
CREATE INDEX idx_events_timestamp_type ON events(timestamp DESC, type);

COMMENT ON TABLE events IS 'Event log for audit trail and "Why?" explanations';
COMMENT ON COLUMN events.cause_chain IS 'Causation chain for explainability';

-- ============================================================================
-- GUEST ACCESS TOKENS (Optional - for temporary guest access)
-- ============================================================================

CREATE TABLE guest_access_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100),  -- Descriptive name like "Babysitter access"
    
    -- Creator
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Expiration
    expires_at TIMESTAMP NOT NULL,
    last_used TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_guest_tokens_token ON guest_access_tokens(token);
CREATE INDEX idx_guest_tokens_expires_at ON guest_access_tokens(expires_at);

COMMENT ON TABLE guest_access_tokens IS 'Temporary access tokens for guests (no account needed)';

-- ============================================================================
-- FUNCTIONS
-- ============================================================================

-- Update timestamp function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Auto-update updated_at columns
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rooms_updated_at BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_esp32_controllers_updated_at BEFORE UPDATE ON esp32_controllers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_led_mappings_updated_at BEFORE UPDATE ON led_mappings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_scenes_updated_at BEFORE UPDATE ON scenes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rules_updated_at BEFORE UPDATE ON rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at BEFORE UPDATE ON schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- CONSTRAINTS & VALIDATION
-- ============================================================================

-- Ensure at least one OWNER exists (cannot delete last OWNER)
CREATE OR REPLACE FUNCTION prevent_last_owner_deletion()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.role = 'OWNER' THEN
        IF (SELECT COUNT(*) FROM users WHERE role = 'OWNER') <= 1 THEN
            RAISE EXCEPTION 'Cannot delete the last OWNER user';
        END IF;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_last_owner_before_delete
    BEFORE DELETE ON users
    FOR EACH ROW
    EXECUTE FUNCTION prevent_last_owner_deletion();

-- Validate LED index is within controller's range
CREATE OR REPLACE FUNCTION validate_led_index()
RETURNS TRIGGER AS $$
DECLARE
    max_leds INTEGER;
BEGIN
    SELECT num_leds INTO max_leds
    FROM esp32_controllers
    WHERE id = NEW.controller_id;
    
    IF NEW.led_index >= max_leds THEN
        RAISE EXCEPTION 'LED index % is out of range for controller (max: %)', 
            NEW.led_index, max_leds - 1;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_led_mapping_index
    BEFORE INSERT OR UPDATE ON led_mappings
    FOR EACH ROW
    EXECUTE FUNCTION validate_led_index();

-- ============================================================================
-- VIEWS
-- ============================================================================

-- View: Devices with full room and controller information
CREATE VIEW v_devices_full AS
SELECT 
    d.id,
    d.name AS device_name,
    d.type,
    r.name AS room_name,
    r.id AS room_id,
    c.device_id AS controller_device_id,
    c.name AS controller_name,
    lm.led_index,
    d.mqtt_cmd_topic,
    d.mqtt_state_topic,
    d.meta_json,
    s.is_on,
    s.brightness_pct,
    s.rgb_color,
    s.last_seen,
    d.created_at
FROM devices d
LEFT JOIN rooms r ON d.room_id = r.id
LEFT JOIN esp32_controllers c ON d.controller_id = c.id
LEFT JOIN led_mappings lm ON d.led_mapping_id = lm.id
LEFT JOIN device_state_latest s ON d.id = s.device_id;

COMMENT ON VIEW v_devices_full IS 'Complete device information with room, controller, and state';

-- View: Unmapped LEDs per controller
CREATE VIEW v_unmapped_leds AS
WITH led_indices AS (
    SELECT 
        c.id AS controller_id,
        c.device_id,
        c.name AS controller_name,
        generate_series(0, c.num_leds - 1) AS led_index
    FROM esp32_controllers c
)
SELECT 
    li.controller_id,
    li.device_id,
    li.controller_name,
    li.led_index
FROM led_indices li
LEFT JOIN led_mappings lm ON li.controller_id = lm.controller_id 
    AND li.led_index = lm.led_index
WHERE lm.id IS NULL
ORDER BY li.controller_id, li.led_index;

COMMENT ON VIEW v_unmapped_leds IS 'LEDs that have not been mapped to rooms yet';

-- ============================================================================
-- GRANTS (if needed for specific database users)
-- ============================================================================
-- NOTE: Using postgres superuser, no explicit grants needed

-- ============================================================================
-- COMPLETION
-- ============================================================================

-- Success message
DO $$ 
BEGIN 
    RAISE NOTICE 'Smart Lighting schema V1 created successfully';
    RAISE NOTICE 'Tables: users, rooms, esp32_controllers, led_mappings, devices, scenes, rules, schedules, events';
    RAISE NOTICE 'Roles: OWNER, MEMBER, GUEST';
END $$;
