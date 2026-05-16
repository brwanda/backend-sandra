-- Verify Chair of Head of Delegation Workflow
-- This script ensures reports from ALL chairs (except Chair of Head of Delegation) 
-- are sent to the Chair of Head of Delegation for approval/rejection

-- 1. Check current Chair of Head of Delegation
SELECT 'Current Chair of Head of Delegation:' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR' 
AND sc.name = 'Head Of Delegation';

-- 2. Check all other chairs (who should submit reports to Chair of Head of Delegation)
SELECT 'All other chairs (should submit reports to Chair of Head of Delegation):' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR' 
AND sc.name != 'Head Of Delegation'
ORDER BY sc.name;

-- 3. Check reports submitted by other chairs
SELECT 'Reports submitted by other chairs (should be reviewed by Chair of Head of Delegation):' as info;
SELECT 
    r.id,
    r.status,
    r.submitted_at,
    res.title as resolution_title,
    sc.name as subcommittee_name,
    u.name as submitted_by,
    u.email as submitted_by_email
FROM reports r
JOIN resolutions res ON r.resolution_id = res.id
JOIN sub_committee sc ON r.subcommittee_id = sc.id
JOIN users u ON r.submitted_by = u.id
WHERE u.role = 'CHAIR' 
AND sc.name != 'Head Of Delegation'
ORDER BY r.submitted_at DESC;

-- 4. Check notifications for Chair of Head of Delegation
SELECT 'Notifications for Chair of Head of Delegation:' as info;
SELECT 
    n.id,
    n.title,
    n.message,
    n.type,
    n.is_read,
    n.created_at,
    u.name as chair_name
FROM notifications n
JOIN users u ON n.user_id = u.id
JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR' 
AND sc.name = 'Head Of Delegation'
ORDER BY n.created_at DESC;

-- 5. Check if Chair of Head of Delegation has reviewed any reports
SELECT 'Reports reviewed by Chair of Head of Delegation:' as info;
SELECT 
    r.id,
    r.status,
    r.hod_comments,
    r.hod_reviewed_at,
    res.title as resolution_title,
    sc.name as subcommittee_name,
    u.name as submitted_by,
    hod.name as reviewed_by_hod
FROM reports r
JOIN resolutions res ON r.resolution_id = res.id
JOIN sub_committee sc ON r.subcommittee_id = sc.id
JOIN users u ON r.submitted_by = u.id
JOIN users hod ON r.reviewed_by_hod = hod.id
JOIN sub_committee hod_sc ON hod.subcommittee_id = hod_sc.id
WHERE hod.role = 'CHAIR' 
AND hod_sc.name = 'Head Of Delegation'
ORDER BY r.hod_reviewed_at DESC;

-- 6. Show workflow statistics
SELECT 'Workflow Statistics:' as info;
SELECT 
    'Total Chair of Head of Delegation' as metric,
    COUNT(*) as value
FROM users u
JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR' AND sc.name = 'Head Of Delegation'
UNION ALL
SELECT 
    'Total other chairs' as metric,
    COUNT(*) as value
FROM users u
JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR' AND sc.name != 'Head Of Delegation'
UNION ALL
SELECT 
    'Reports from other chairs' as metric,
    COUNT(*) as value
FROM reports r
JOIN users u ON r.submitted_by = u.id
JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR' AND sc.name != 'Head Of Delegation'
UNION ALL
SELECT 
    'Reports pending Chair of Head of Delegation review' as metric,
    COUNT(*) as value
FROM reports r
WHERE r.status = 'SUBMITTED'
UNION ALL
SELECT 
    'Reports approved by Chair of Head of Delegation' as metric,
    COUNT(*) as value
FROM reports r
JOIN users hod ON r.reviewed_by_hod = hod.id
JOIN sub_committee hod_sc ON hod.subcommittee_id = hod_sc.id
WHERE r.status = 'APPROVED_BY_HOD' 
AND hod.role = 'CHAIR' 
AND hod_sc.name = 'Head Of Delegation'
UNION ALL
SELECT 
    'Reports rejected by Chair of Head of Delegation' as metric,
    COUNT(*) as value
FROM reports r
JOIN users hod ON r.reviewed_by_hod = hod.id
JOIN sub_committee hod_sc ON hod.subcommittee_id = hod_sc.id
WHERE r.status = 'REJECTED_BY_HOD' 
AND hod.role = 'CHAIR' 
AND hod_sc.name = 'Head Of Delegation';

-- 7. Verify the workflow is working correctly
SELECT 'Workflow Verification:' as info;
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM users u 
            JOIN sub_committee sc ON u.subcommittee_id = sc.id 
            WHERE u.role = 'CHAIR' AND sc.name = 'Head Of Delegation'
        ) THEN '✅ Chair of Head of Delegation exists'
        ELSE '❌ Chair of Head of Delegation missing'
    END as check_result
UNION ALL
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM users u 
            JOIN sub_committee sc ON u.subcommittee_id = sc.id 
            WHERE u.role = 'CHAIR' AND sc.name != 'Head Of Delegation'
        ) THEN '✅ Other chairs exist'
        ELSE '❌ No other chairs found'
    END as check_result
UNION ALL
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM reports r 
            JOIN users u ON r.submitted_by = u.id 
            JOIN sub_committee sc ON u.subcommittee_id = sc.id 
            WHERE u.role = 'CHAIR' AND sc.name != 'Head Of Delegation'
        ) THEN '✅ Reports from other chairs exist'
        ELSE '❌ No reports from other chairs'
    END as check_result
UNION ALL
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM reports r 
            WHERE r.status = 'SUBMITTED'
        ) THEN '✅ Reports pending review exist'
        ELSE '❌ No reports pending review'
    END as check_result;
