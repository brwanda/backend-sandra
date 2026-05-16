-- Migration to fix minutes length and link sub-tasks to meetings

-- 1. Change meetings.minutes from VARCHAR(255) to TEXT
ALTER TABLE meetings ALTER COLUMN minutes TYPE TEXT;

-- 2. Add meeting_id to sub_tasks table
-- First check if the column exists to avoid errors
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sub_tasks' AND column_name = 'meeting_id') THEN
        ALTER TABLE sub_tasks ADD COLUMN meeting_id BIGINT;
        ALTER TABLE sub_tasks ADD CONSTRAINT fk_sub_tasks_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id);
    END IF;
END $$;
