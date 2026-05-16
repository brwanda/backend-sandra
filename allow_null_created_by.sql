-- Migration to allow NULL for created_by columns in meetings and resolutions tables
-- This allows users to be deleted without losing meeting/resolution history

-- Allow NULL for created_by in meetings table
ALTER TABLE meetings 
ALTER COLUMN created_by DROP NOT NULL;

-- Allow NULL for created_by in resolutions table
ALTER TABLE resolutions 
ALTER COLUMN created_by DROP NOT NULL;

-- Verify the changes
SELECT 
    table_name, 
    column_name, 
    is_nullable 
FROM information_schema.columns 
WHERE table_name IN ('meetings', 'resolutions') 
  AND column_name = 'created_by';
