-- Make resolution_id nullable in sub_tasks table
-- This allows tasks to exist without being linked to a resolution
-- (e.g., standalone tasks created for technical meetings)

ALTER TABLE sub_tasks 
ALTER COLUMN resolution_id DROP NOT NULL;

-- Verify the change
SELECT column_name, is_nullable, data_type 
FROM information_schema.columns 
WHERE table_name = 'sub_tasks' AND column_name = 'resolution_id';
