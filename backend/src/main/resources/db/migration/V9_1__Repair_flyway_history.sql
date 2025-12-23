-- V9_1: Repair Flyway history for V9 migration
-- This is a no-op migration that allows Flyway to mark V9 as complete after fixing

SET search_path TO smartlighting;

-- Ensure all V9 objects exist (idempotent creates)
-- Scenes table
CREATE TABLE IF NOT EXISTS scenes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50) DEFAULT '💡',
    settings_json JSONB NOT NULL DEFAULT '{}',
    is_preset BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Schedules table
CREATE TABLE IF NOT EXISTS schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    trigger_type VARCHAR(20) NOT NULL,
    trigger_config JSONB NOT NULL DEFAULT '{}',
    conditions JSONB DEFAULT '[]',
    actions JSONB NOT NULL DEFAULT '[]',
    last_triggered_at TIMESTAMP,
    trigger_count INTEGER DEFAULT 0,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- NLP Commands table
CREATE TABLE IF NOT EXISTS nlp_commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_input TEXT NOT NULL,
    parsed_json JSONB,
    executed BOOLEAN DEFAULT FALSE,
    execution_result TEXT,
    is_scheduled BOOLEAN DEFAULT FALSE,
    schedule_id UUID REFERENCES schedules(id),
    user_id UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add unique constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'scenes_name_unique') THEN
        ALTER TABLE scenes ADD CONSTRAINT scenes_name_unique UNIQUE (name);
    END IF;
END $$;

-- All indexes (IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_scenes_name ON scenes(name);
CREATE INDEX IF NOT EXISTS idx_scenes_is_preset ON scenes(is_preset);
CREATE INDEX IF NOT EXISTS idx_scenes_is_active ON scenes(is_active);
CREATE INDEX IF NOT EXISTS idx_schedules_enabled ON schedules(enabled);
CREATE INDEX IF NOT EXISTS idx_schedules_trigger_type ON schedules(trigger_type);
CREATE INDEX IF NOT EXISTS idx_nlp_commands_user ON nlp_commands(user_id);
CREATE INDEX IF NOT EXISTS idx_nlp_commands_created ON nlp_commands(created_at DESC);

-- Seed scenes if not exists
INSERT INTO scenes (name, description, icon, settings_json, is_preset, is_active)
SELECT * FROM (VALUES
    ('Movie Night', 'Dim, warm lighting perfect for watching movies', '🎬', '{"target": "all", "brightness": 15, "rgb": [255, 147, 41], "color_temp": 2700}'::jsonb, TRUE, TRUE),
    ('Morning Wake Up', 'Bright, energizing light to start your day', '☀️', '{"target": "all", "brightness": 80, "rgb": [255, 244, 229], "color_temp": 5000}'::jsonb, TRUE, TRUE),
    ('Relaxation', 'Soft, calming ambiance for unwinding', '🧘', '{"target": "all", "brightness": 30, "rgb": [255, 182, 108], "color_temp": 3000}'::jsonb, TRUE, TRUE),
    ('Party Mode', 'Vibrant, colorful lighting for celebrations', '🎉', '{"target": "all", "brightness": 100, "rgb": [255, 0, 128], "color_temp": 4000}'::jsonb, TRUE, TRUE),
    ('Focus / Work', 'Bright, neutral light for productivity', '💼', '{"target": "all", "brightness": 90, "rgb": [255, 255, 255], "color_temp": 4500}'::jsonb, TRUE, TRUE),
    ('Night Light', 'Very dim, warm glow for nighttime', '🌙', '{"target": "all", "brightness": 5, "rgb": [255, 100, 50], "color_temp": 2200}'::jsonb, TRUE, TRUE)
) AS new_scenes(name, description, icon, settings_json, is_preset, is_active)
WHERE NOT EXISTS (SELECT 1 FROM scenes WHERE scenes.name = new_scenes.name);

-- Done
DO $$ BEGIN RAISE NOTICE 'V9_1: Repair migration completed successfully'; END $$;

