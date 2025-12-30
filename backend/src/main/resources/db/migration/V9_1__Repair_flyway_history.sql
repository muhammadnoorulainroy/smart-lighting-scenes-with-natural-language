-- V9_1: Repair Flyway history for V9 migration
-- This is a no-op migration that allows Flyway to mark V9 as complete after fixing

SET search_path TO smartlighting;

-- Ensure all V9 objects exist (idempotent creates)
-- Scenes table - handle both new creation and existing table migration
DO $$
BEGIN
    -- Create table if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'smartlighting' AND table_name = 'scenes') THEN
        CREATE TABLE scenes (
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
    ELSE
        -- Add missing columns if table exists (from V1 schema)
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'icon') THEN
            ALTER TABLE scenes ADD COLUMN icon VARCHAR(50) DEFAULT '💡';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'settings_json') THEN
            ALTER TABLE scenes ADD COLUMN settings_json JSONB DEFAULT '{}';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'is_preset') THEN
            ALTER TABLE scenes ADD COLUMN is_preset BOOLEAN DEFAULT FALSE;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'is_active') THEN
            ALTER TABLE scenes ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'created_by') THEN
            ALTER TABLE scenes ADD COLUMN created_by UUID REFERENCES users(id);
        END IF;
    END IF;
END $$;

-- Schedules table - handle both new creation and existing table migration
DO $$
BEGIN
    -- Create table if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'smartlighting' AND table_name = 'schedules') THEN
        CREATE TABLE schedules (
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
    ELSE
        -- Add missing columns if table exists (from V1 schema)
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'name') THEN
            ALTER TABLE schedules ADD COLUMN name VARCHAR(100);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'description') THEN
            ALTER TABLE schedules ADD COLUMN description TEXT;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'enabled') THEN
            ALTER TABLE schedules ADD COLUMN enabled BOOLEAN DEFAULT TRUE;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'trigger_type') THEN
            ALTER TABLE schedules ADD COLUMN trigger_type VARCHAR(20);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'trigger_config') THEN
            ALTER TABLE schedules ADD COLUMN trigger_config JSONB DEFAULT '{}';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'conditions') THEN
            ALTER TABLE schedules ADD COLUMN conditions JSONB DEFAULT '[]';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'actions') THEN
            ALTER TABLE schedules ADD COLUMN actions JSONB DEFAULT '[]';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'last_triggered_at') THEN
            ALTER TABLE schedules ADD COLUMN last_triggered_at TIMESTAMP;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'trigger_count') THEN
            ALTER TABLE schedules ADD COLUMN trigger_count INTEGER DEFAULT 0;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'created_by') THEN
            ALTER TABLE schedules ADD COLUMN created_by UUID REFERENCES users(id);
        END IF;
    END IF;
END $$;

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

-- All indexes (IF NOT EXISTS) - only create if columns exist
CREATE INDEX IF NOT EXISTS idx_scenes_name ON scenes(name);
DO $$
BEGIN
    -- Only create index on is_preset if column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'is_preset') THEN
        CREATE INDEX IF NOT EXISTS idx_scenes_is_preset ON scenes(is_preset);
    END IF;
    -- Only create index on is_active if column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'scenes' AND column_name = 'is_active') THEN
        CREATE INDEX IF NOT EXISTS idx_scenes_is_active ON scenes(is_active);
    END IF;
    -- Only create index on schedules.enabled if column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'enabled') THEN
        CREATE INDEX IF NOT EXISTS idx_schedules_enabled ON schedules(enabled);
    END IF;
    -- Only create index on schedules.trigger_type if column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartlighting' AND table_name = 'schedules' AND column_name = 'trigger_type') THEN
        CREATE INDEX IF NOT EXISTS idx_schedules_trigger_type ON schedules(trigger_type);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_nlp_commands_user ON nlp_commands(user_id);
CREATE INDEX IF NOT EXISTS idx_nlp_commands_created ON nlp_commands(created_at DESC);

-- Skip seeding scenes in this migration
-- V10 will recreate tables cleanly and handle seeding properly
-- This migration only ensures schema compatibility

-- Done
DO $$ BEGIN RAISE NOTICE 'V9_1: Repair migration completed successfully'; END $$;

