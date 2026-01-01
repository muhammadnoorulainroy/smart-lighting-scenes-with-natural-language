-- Add test users for local development
-- Password for all users: "password"
-- Uses pgcrypto extension to generate bcrypt hash at runtime

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, email, name, role, provider, password_hash, is_active, created_at, updated_at)
VALUES 
    (
        'a0000000-0000-0000-0000-000000000001',
        'owner@test.com',
        'Test Owner',
        'OWNER',
        'LOCAL',
        crypt('password', gen_salt('bf', 10)),
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'a0000000-0000-0000-0000-000000000002',
        'resident@test.com',
        'Test Resident',
        'RESIDENT',
        'LOCAL',
        crypt('password', gen_salt('bf', 10)),
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'a0000000-0000-0000-0000-000000000003',
        'guest@test.com',
        'Test Guest',
        'GUEST',
        'LOCAL',
        crypt('password', gen_salt('bf', 10)),
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (email) DO NOTHING;