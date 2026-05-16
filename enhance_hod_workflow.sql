-- Enhance HOD Workflow and Fix Missing Data
-- This script ensures the complete HOD workflow is working properly

-- 1. Check current HOD users
SELECT 'Current HOD users:' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'HOD';

-- 2. Check if Head of Delegation subcommittee exists
SELECT 'Head of Delegation subcommittee:' as info;
SELECT id, name FROM sub_committee WHERE name = 'Head Of Delegation';

-- 3. Fix HOD users who don't have subcommittee assignment
UPDATE users 
SET subcommittee_id = (
    SELECT id FROM sub_committee WHERE name = 'Head Of Delegation' LIMIT 1
)
WHERE users.role = 'HOD' 
AND users.subcommittee_id IS NULL;

-- 4. Check reports that need HOD review
SELECT 'Reports pending HOD review:' as info;
SELECT 
    r.id,
    r.status,
    r.submitted_at,
    res.title as resolution_title,
    sc.name as subcommittee_name,
    u.name as submitted_by
FROM reports r
JOIN resolutions res ON r.resolution_id = res.id
JOIN sub_committee sc ON r.subcommittee_id = sc.id
JOIN users u ON r.submitted_by = u.id
WHERE r.status = 'SUBMITTED'
ORDER BY r.submitted_at DESC;

-- 5. Check notifications for HOD users
SELECT 'Notifications for HOD users:' as info;
SELECT 
    n.id,
    n.title,
    n.message,
    n.type,
    n.is_read,
    n.created_at,
    u.name as hod_name
FROM notifications n
JOIN users u ON n.user_id = u.id
WHERE u.role = 'HOD'
ORDER BY n.created_at DESC;

-- 6. Create test data for HOD workflow (if needed)
-- Insert a test HOD user if none exists
INSERT INTO users (email, password, name, role, subcommittee_id, is_active, created_at)
SELECT 
    'hod@eara.org',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'Head of Delegation',
    'HOD',
    (SELECT id FROM sub_committee WHERE name = 'Head Of Delegation' LIMIT 1),
    true,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE role = 'HOD'
);

-- 7. Verify the fix
SELECT 'After fix - HOD users:' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'HOD';

-- 8. Show workflow status
SELECT 'HOD Workflow Status:' as info;
SELECT 
    'Total HOD users' as metric,
    COUNT(*) as value
FROM users 
WHERE role = 'HOD'
UNION ALL
SELECT 
    'HOD users with subcommittee' as metric,
    COUNT(*) as value
FROM users 
WHERE role = 'HOD' AND subcommittee_id IS NOT NULL
UNION ALL
SELECT 
    'Reports pending review' as metric,
    COUNT(*) as value
FROM reports 
WHERE status = 'SUBMITTED'
UNION ALL
SELECT 
    'Notifications for HOD' as metric,
    COUNT(*) as value
FROM notifications n
JOIN users u ON n.user_id = u.id
WHERE u.role = 'HOD' AND n.is_read = false;
