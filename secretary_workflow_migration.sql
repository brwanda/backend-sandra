-- =====================================================
-- SECRETARY WORKFLOW ENHANCEMENT MIGRATION
-- =====================================================
-- This migration adds support for the new secretary workflow:
-- 1. Delegation Secretary: CG meetings create resolutions only
-- 2. Delegation Secretary: TC meetings create tasks (title only) + AOB items
-- 3. Committee Secretary: Subcommittee meetings describe TC tasks + set deadlines
-- =====================================================

-- Add AOB (Any Other Business) items to meetings table
-- This allows Delegation Secretary to save multiple AOB items during TC minutes
ALTER TABLE meetings 
ADD COLUMN aob_items TEXT COMMENT 'JSON array of Any Other Business items for TC meetings';

-- Add source meeting type to sub_tasks table
-- This tracks whether a task originated from a TC meeting or other source
ALTER TABLE sub_tasks 
ADD COLUMN source_meeting_type VARCHAR(50) COMMENT 'Meeting type where task was created (TECHNICAL_MEETING, etc)';

-- Add flag to indicate if task requires description from Committee Secretary
-- TC tasks created by Delegation Secretary will have this set to TRUE
ALTER TABLE sub_tasks 
ADD COLUMN requires_description BOOLEAN DEFAULT FALSE COMMENT 'TRUE if task awaits description from Committee Secretary';

-- Add index for efficient querying of tasks awaiting description
CREATE INDEX idx_sub_tasks_requires_description 
ON sub_tasks(requires_description, subcommittee_id) 
WHERE requires_description = TRUE;

-- Add index for source meeting type queries
CREATE INDEX idx_sub_tasks_source_meeting_type 
ON sub_tasks(source_meeting_type);

-- =====================================================
-- DATA MIGRATION (Optional - for existing data)
-- =====================================================

-- Mark existing tasks without description as requiring description if from TC meetings
UPDATE sub_tasks st
JOIN meetings m ON st.meeting_id = m.id
SET st.requires_description = TRUE,
    st.source_meeting_type = 'TECHNICAL_MEETING'
WHERE m.meeting_type = 'TECHNICAL_MEETING'
  AND (st.description IS NULL OR st.description = '')
  AND st.deadline IS NULL;

-- Mark existing tasks with description as complete (no description needed)
UPDATE sub_tasks
SET requires_description = FALSE
WHERE description IS NOT NULL 
  AND description != '';

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Verify AOB column added
SELECT COUNT(*) as meetings_with_aob_column
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'meetings' 
  AND COLUMN_NAME = 'aob_items';

-- Verify SubTask columns added
SELECT COUNT(*) as subtask_new_columns
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'sub_tasks' 
  AND COLUMN_NAME IN ('source_meeting_type', 'requires_description');

-- Show tasks requiring description by subcommittee
SELECT 
    sc.name as subcommittee_name,
    COUNT(*) as tasks_awaiting_description
FROM sub_tasks st
JOIN sub_committees sc ON st.subcommittee_id = sc.id
WHERE st.requires_description = TRUE
GROUP BY sc.id, sc.name
ORDER BY tasks_awaiting_description DESC;

-- =====================================================
-- ROLLBACK SCRIPT (if needed)
-- =====================================================
-- DROP INDEX idx_sub_tasks_requires_description ON sub_tasks;
-- DROP INDEX idx_sub_tasks_source_meeting_type ON sub_tasks;
-- ALTER TABLE sub_tasks DROP COLUMN requires_description;
-- ALTER TABLE sub_tasks DROP COLUMN source_meeting_type;
-- ALTER TABLE meetings DROP COLUMN aob_items;
