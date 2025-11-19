-- Smart Lighting Database Initialization
-- PostgreSQL 16
-- 
-- NOTE: This script is only for Docker container initialization.
-- The actual schema is managed by Flyway migrations in:
-- backend/src/main/resources/db/migration/
--
-- This script:
-- 1. Creates the smartlighting schema
-- 2. Sets up basic prerequisites
-- 3. Flyway will handle all table creation and migrations

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS smartlighting;

-- Set search path
ALTER DATABASE smartlighting SET search_path TO smartlighting, public;

-- Grant schema usage to the smartlighting user
GRANT ALL PRIVILEGES ON SCHEMA smartlighting TO smartlighting;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA smartlighting TO smartlighting;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA smartlighting TO smartlighting;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA smartlighting 
    GRANT ALL PRIVILEGES ON TABLES TO smartlighting;
ALTER DEFAULT PRIVILEGES IN SCHEMA smartlighting 
    GRANT ALL PRIVILEGES ON SEQUENCES TO smartlighting;

-- Success message
DO $$ 
BEGIN 
    RAISE NOTICE '==================================================';
    RAISE NOTICE 'Smart Lighting Database Initialized';
    RAISE NOTICE 'Schema: smartlighting';
    RAISE NOTICE 'Database ready for Flyway migrations';
    RAISE NOTICE '==================================================';
END $$;
