-- V5__Add_second_owner.sql
-- Add additional owner email

SET search_path TO smartlighting;

-- Update role to OWNER for the new email (works for existing users)
UPDATE users 
SET role = 'OWNER', 
    is_active = true,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'muhammadnoorulain2@gmail.com';

-- Update the trigger function to include both owner emails
CREATE OR REPLACE FUNCTION set_owner_role_on_insert()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email IN ('nimrafayaz9@gmail.com', 'muhammadnoorulain2@gmail.com') THEN
        NEW.role := 'OWNER';
        NEW.is_active := true;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Log the change
DO $$ 
BEGIN 
    RAISE NOTICE 'Owner role configured for: muhammadnoorulain2@gmail.com';
END $$;

