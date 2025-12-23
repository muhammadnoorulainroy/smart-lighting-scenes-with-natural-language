-- V7__Update_device_types.sql
-- Update device_type enum to include new types

SET search_path TO smartlighting;

-- Add new values to the device_type enum
-- PostgreSQL allows adding values to existing enums
ALTER TYPE device_type ADD VALUE IF NOT EXISTS 'MULTI_SENSOR';
ALTER TYPE device_type ADD VALUE IF NOT EXISTS 'LED';
ALTER TYPE device_type ADD VALUE IF NOT EXISTS 'MICROCONTROLLER';

-- Make room_id nullable for microcontrollers
ALTER TABLE devices ALTER COLUMN room_id DROP NOT NULL;

-- Note: 'LIGHT' maps to 'LED', 'SENSOR' stays the same
-- 'SWITCH' can be deprecated or kept for legacy support

