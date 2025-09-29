-- Smart Lighting Database Schema
-- PostgreSQL 16

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS smartlighting;
SET search_path TO smartlighting;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- User roles enum
CREATE TYPE user_role AS ENUM ('ADMIN', 'RESIDENT', 'GUEST');

-- OAuth providers enum
CREATE TYPE oauth_provider AS ENUM ('GOOGLE', 'LOCAL');

-- Device types enum
CREATE TYPE device_type AS ENUM ('LIGHT', 'SENSOR', 'SWITCH');

-- Event types enum
CREATE TYPE event_type AS ENUM (
    'DEVICE_STATE_CHANGE',
    'SCENE_APPLIED',
    'RULE_TRIGGERED',
    'MANUAL_CONTROL',
    'CONFLICT_DETECTED',
    'SYSTEM_EVENT'
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    picture_url TEXT,
    role user_role NOT NULL DEFAULT 'GUEST',
    provider oauth_provider NOT NULL DEFAULT 'GOOGLE',
    provider_sub VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_sub)
);

-- Rooms table
CREATE TABLE IF NOT EXISTS rooms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Devices table
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,
    type device_type NOT NULL,
    name VARCHAR(100) NOT NULL,
    mqtt_cmd_topic VARCHAR(255) NOT NULL,
    mqtt_state_topic VARCHAR(255) NOT NULL,
    meta_json JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(room_id, name)
);

-- Device state (latest) table
CREATE TABLE IF NOT EXISTS device_state_latest (
    device_id UUID PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
    is_on BOOLEAN DEFAULT false,
    brightness_pct INTEGER CHECK (brightness_pct >= 0 AND brightness_pct <= 100),
    color_temp_mired INTEGER CHECK (color_temp_mired >= 153 AND color_temp_mired <= 500),
    rgb_color VARCHAR(7), -- Hex color code
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Sensor readings (history) table
CREATE TABLE IF NOT EXISTS sensor_readings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metric VARCHAR(50) NOT NULL, -- 'lux', 'temperature', 'humidity', etc.
    value DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Scenes table
CREATE TABLE IF NOT EXISTS scenes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    actions_json JSONB NOT NULL DEFAULT '[]',
    is_global BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, owner_id)
);

-- Rules table (from natural language processing)
CREATE TABLE IF NOT EXISTS rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    priority INTEGER DEFAULT 50,
    json_dsl JSONB NOT NULL,
    natural_language_input TEXT,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Schedules table (compiled from rules)
CREATE TABLE IF NOT EXISTS schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID REFERENCES rules(id) ON DELETE CASCADE,
    compiled_spec_json JSONB NOT NULL,
    next_fire_at TIMESTAMP WITH TIME ZONE,
    last_fired_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Events/Logs table
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    type event_type NOT NULL,
    details_json JSONB DEFAULT '{}',
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    rule_id UUID REFERENCES rules(id) ON DELETE SET NULL,
    scene_id UUID REFERENCES scenes(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_sub ON users(provider, provider_sub);
CREATE INDEX idx_devices_room_id ON devices(room_id);
CREATE INDEX idx_device_state_device_id ON device_state_latest(device_id);
CREATE INDEX idx_sensor_readings_device_timestamp ON sensor_readings(device_id, timestamp DESC);
CREATE INDEX idx_scenes_owner ON scenes(owner_id);
CREATE INDEX idx_rules_owner ON rules(owner_id);
CREATE INDEX idx_schedules_rule ON schedules(rule_id);
CREATE INDEX idx_schedules_next_fire ON schedules(next_fire_at);
CREATE INDEX idx_events_timestamp ON events(timestamp DESC);
CREATE INDEX idx_events_type ON events(type);
CREATE INDEX idx_events_actor ON events(actor_user_id);
CREATE INDEX idx_events_device ON events(device_id);

-- Triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rooms_updated_at BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_device_state_latest_updated_at BEFORE UPDATE ON device_state_latest
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_scenes_updated_at BEFORE UPDATE ON scenes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rules_updated_at BEFORE UPDATE ON rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at BEFORE UPDATE ON schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
