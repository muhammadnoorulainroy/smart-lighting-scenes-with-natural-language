-- Smart Lighting Database Schema
-- Initial migration

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS smartlighting;
SET search_path TO smartlighting;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- User roles enum
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'RESIDENT', 'GUEST');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- OAuth providers enum
DO $$ BEGIN
    CREATE TYPE oauth_provider AS ENUM ('GOOGLE', 'LOCAL');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Device types enum
DO $$ BEGIN
    CREATE TYPE device_type AS ENUM ('LIGHT', 'SENSOR', 'SWITCH');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Event types enum
DO $$ BEGIN
    CREATE TYPE event_type AS ENUM (
        'DEVICE_STATE_CHANGE',
        'SCENE_APPLIED',
        'RULE_TRIGGERED',
        'MANUAL_CONTROL',
        'CONFLICT_DETECTED',
        'SYSTEM_EVENT'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

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

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_provider_sub ON users(provider, provider_sub);
CREATE INDEX IF NOT EXISTS idx_devices_room_id ON devices(room_id);
CREATE INDEX IF NOT EXISTS idx_device_state_device_id ON device_state_latest(device_id);
CREATE INDEX IF NOT EXISTS idx_scenes_owner ON scenes(owner_id);

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
