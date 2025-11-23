-- V3__seed_owner_user.sql
-- Set owner role for specific email address

SET search_path TO smartlighting;

-- Update role to OWNER for the specified email (works for existing users)
UPDATE users 
SET role = 'OWNER', 
    is_active = true,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'nimrafayaz9@gmail.com';

-- Create function to automatically set OWNER role for this email on insert
CREATE OR REPLACE FUNCTION set_owner_role_on_insert()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email = 'nimrafayaz9@gmail.com' THEN
        NEW.role := 'OWNER';
        NEW.is_active := true;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to set OWNER role when user is created via OAuth
CREATE TRIGGER set_owner_role_trigger
    BEFORE INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_owner_role_on_insert();

-- Also update role if user already exists
DO $$ 
BEGIN 
    UPDATE users 
    SET role = 'OWNER', is_active = true
    WHERE email = 'nimrafayaz9@gmail.com';
    
    RAISE NOTICE 'Owner role configured for: nimrafayaz9@gmail.com';
END $$;

