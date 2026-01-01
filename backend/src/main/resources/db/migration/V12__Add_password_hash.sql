-- Add password_hash column for local authentication
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password for local authentication (null for OAuth users)';

