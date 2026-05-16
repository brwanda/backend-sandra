-- Clears only meeting workflow data to allow a fresh start.
-- This script intentionally does NOT modify users or reference/master tables.

BEGIN;

-- Pre-cleanup counts
SELECT 'meetings' AS table_name, COUNT(*) AS row_count FROM meetings
UNION ALL
SELECT 'resolutions', COUNT(*) FROM resolutions
UNION ALL
SELECT 'resolution_assignments', COUNT(*) FROM resolution_assignments
UNION ALL
SELECT 'sub_tasks', COUNT(*) FROM sub_tasks
UNION ALL
SELECT 'reports', COUNT(*) FROM reports
UNION ALL
SELECT 'meeting_invitations', COUNT(*) FROM meeting_invitations
UNION ALL
SELECT 'attendance', COUNT(*) FROM attendance
ORDER BY table_name;

-- Clear workflow data and reset IDs for fresh creation
TRUNCATE TABLE
  attendance,
  meeting_invitations,
  reports,
  sub_tasks,
  resolution_assignments,
  resolutions,
  meetings
RESTART IDENTITY;

-- Post-cleanup counts
SELECT 'meetings' AS table_name, COUNT(*) AS row_count FROM meetings
UNION ALL
SELECT 'resolutions', COUNT(*) FROM resolutions
UNION ALL
SELECT 'resolution_assignments', COUNT(*) FROM resolution_assignments
UNION ALL
SELECT 'sub_tasks', COUNT(*) FROM sub_tasks
UNION ALL
SELECT 'reports', COUNT(*) FROM reports
UNION ALL
SELECT 'meeting_invitations', COUNT(*) FROM meeting_invitations
UNION ALL
SELECT 'attendance', COUNT(*) FROM attendance
ORDER BY table_name;

COMMIT;
