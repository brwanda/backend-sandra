-- =====================================================================
-- SAFE MIGRATION: Move Head Of Delegation from sub_committee to committee
-- =====================================================================
-- This fixes the issue where HOD doesn't appear in the Committee Member dropdown
-- when creating new members. HOD should be a committee-level entity like 
-- Commissioner General, not a sub-committee.
--
-- SAFETY FEATURES:
-- - Uses transactions (can rollback on error)
-- - Checks for existing data before inserting
-- - Preserves all member data and relationships
-- - Provides detailed logging at each step
-- =====================================================================

-- Start transaction
BEGIN;

-- =====================================================================
-- STEP 1: PRE-MIGRATION CHECKS
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'STEP 1: PRE-MIGRATION STATE CHECK';
    RAISE NOTICE '========================================';
END $$;

-- Check current HOD in sub_committee table
SELECT 
    '1.1 Current HOD in sub_committee table:' as step,
    id, 
    subcommittee_name, 
    parent_committee_id 
FROM sub_committee 
WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' 
   OR LOWER(subcommittee_name) = 'hod';

-- Check current committees
SELECT 
    '1.2 Current committees:' as step,
    id, 
    committee_name 
FROM committee 
ORDER BY id;

-- Check HOD members in csub_committee_members
SELECT 
    '1.3 HOD members in csub_committee_members:' as step,
    csm.id,
    csm.name,
    csm.email,
    csm.phone,
    sc.subcommittee_name,
    csm.chair as is_chair,
    csm.vice_chair as is_vice_chair,
    csm.committee_secretary as is_committee_secretary,
    csm.committee_member as is_committee_member,
    csm.secretary_of_delegation as is_delegation_secretary
FROM csub_committee_members csm
JOIN sub_committee sc ON csm.position_in_ear = sc.id
WHERE LOWER(sc.subcommittee_name) LIKE '%head%delegation%' 
   OR LOWER(sc.subcommittee_name) = 'hod';

-- Check HOD users
SELECT 
    '1.4 Current HOD users:' as step,
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'HOD';

-- =====================================================================
-- STEP 2: CREATE HOD COMMITTEE
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'STEP 2: CREATE HOD COMMITTEE';
    RAISE NOTICE '========================================';
END $$;

-- Insert HOD into committee table if it doesn't exist
INSERT INTO committee (committee_name)
SELECT 'Head Of Delegation'
WHERE NOT EXISTS (
    SELECT 1 FROM committee WHERE committee_name = 'Head Of Delegation'
);

-- Verify HOD committee was created
SELECT 
    '2.1 HOD committee created/exists:' as step,
    id, 
    committee_name 
FROM committee 
WHERE committee_name = 'Head Of Delegation';

-- =====================================================================
-- STEP 3: MIGRATE HOD MEMBERS TO country_committee_member
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'STEP 3: MIGRATE HOD MEMBERS';
    RAISE NOTICE '========================================';
END $$;

-- Move HOD members from csub_committee_members to country_committee_member
INSERT INTO country_committee_member (
    name,
    email,
    phone,
    country_id,
    committee_id,
    chair,
    vice_chair,
    committee_secretary,
    committee_member,
    delegation_secretary
)
SELECT 
    csm.name,
    csm.email,
    csm.phone,
    csm.country_id,
    (SELECT id FROM committee WHERE committee_name = 'Head Of Delegation'),
    csm.chair,
    csm.vice_chair,
    csm.committee_secretary,
    csm.committee_member,
    csm.secretary_of_delegation
FROM csub_committee_members csm
JOIN sub_committee sc ON csm.position_in_ear = sc.id
WHERE (LOWER(sc.subcommittee_name) LIKE '%head%delegation%' OR LOWER(sc.subcommittee_name) = 'hod')
AND NOT EXISTS (
    SELECT 1 FROM country_committee_member ccm 
    WHERE ccm.email = csm.email 
    AND ccm.committee_id = (SELECT id FROM committee WHERE committee_name = 'Head Of Delegation')
);

-- Verify members were migrated
SELECT 
    '3.1 Migrated HOD members in country_committee_member:' as step,
    ccm.id,
    ccm.name,
    ccm.email,
    c.committee_name,
    ccm.chair as is_chair,
    ccm.vice_chair as is_vice_chair
FROM country_committee_member ccm
JOIN committee c ON ccm.committee_id = c.id
WHERE c.committee_name = 'Head Of Delegation';

-- =====================================================================
-- STEP 4: UPDATE USERS TABLE
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'STEP 4: UPDATE USERS TABLE';
    RAISE NOTICE '========================================';
END $$;

-- Update users table to remove subcommittee_id for HOD users
-- (HOD users should not have subcommittee_id since they're committee-level)
UPDATE users 
SET subcommittee_id = NULL
WHERE role = 'HOD';

-- Verify users were updated
SELECT 
    '4.1 Updated HOD users (subcommittee_id should be NULL):' as step,
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id
FROM users u
WHERE u.role = 'HOD';

-- =====================================================================
-- STEP 5: CLEANUP - REMOVE OLD HOD DATA
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'STEP 5: CLEANUP OLD HOD DATA';
    RAISE NOTICE '========================================';
END $$;

-- Delete HOD members from csub_committee_members (they're now in country_committee_member)
DELETE FROM csub_committee_members
WHERE position_in_ear IN (
    SELECT id FROM sub_committee 
    WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod'
);

-- Delete HOD from sub_committee table (it's now in committee table)
DELETE FROM sub_committee 
WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod';

-- Verify cleanup
SELECT 
    '5.1 Remaining HOD in sub_committee (should be 0):' as step,
    COUNT(*) as count
FROM sub_committee 
WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod';

SELECT 
    '5.2 Remaining HOD members in csub_committee_members (should be 0):' as step,
    COUNT(*) as count
FROM csub_committee_members csm
LEFT JOIN sub_committee sc ON csm.position_in_ear = sc.id
WHERE LOWER(sc.subcommittee_name) LIKE '%head%delegation%' OR LOWER(sc.subcommittee_name) = 'hod';

-- =====================================================================
-- STEP 6: POST-MIGRATION VERIFICATION
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'STEP 6: POST-MIGRATION VERIFICATION';
    RAISE NOTICE '========================================';
END $$;

-- Final verification
SELECT 
    '6.1 HOD committee exists:' as step,
    id, 
    committee_name 
FROM committee 
WHERE committee_name = 'Head Of Delegation';

SELECT 
    '6.2 HOD members in country_committee_member:' as step,
    ccm.id,
    ccm.name,
    ccm.email,
    c.committee_name
FROM country_committee_member ccm
JOIN committee c ON ccm.committee_id = c.id
WHERE c.committee_name = 'Head Of Delegation';

SELECT 
    '6.3 HOD users with correct setup:' as step,
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id
FROM users u
WHERE u.role = 'HOD';

-- =====================================================================
-- MIGRATION SUMMARY
-- =====================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRATION SUMMARY';
    RAISE NOTICE '========================================';
END $$;

SELECT 
    'MIGRATION SUMMARY' as report,
    metric,
    value
FROM (
    SELECT 
        1 as sort_order,
        'Total committees' as metric,
        COUNT(*)::text as value
    FROM committee
    
    UNION ALL
    
    SELECT 
        2 as sort_order,
        'HOD committee exists' as metric,
        CASE WHEN COUNT(*) > 0 THEN 'YES ✓' ELSE 'NO ✗' END as value
    FROM committee 
    WHERE committee_name = 'Head Of Delegation'
    
    UNION ALL
    
    SELECT 
        3 as sort_order,
        'HOD members migrated' as metric,
        COUNT(*)::text as value
    FROM country_committee_member ccm
    JOIN committee c ON ccm.committee_id = c.id
    WHERE c.committee_name = 'Head Of Delegation'
    
    UNION ALL
    
    SELECT 
        4 as sort_order,
        'HOD users updated' as metric,
        COUNT(*)::text as value
    FROM users 
    WHERE role = 'HOD'
    
    UNION ALL
    
    SELECT 
        5 as sort_order,
        'Old HOD sub_committee removed' as metric,
        CASE WHEN COUNT(*) = 0 THEN 'YES ✓' ELSE 'NO ✗ (Still exists!)' END as value
    FROM sub_committee 
    WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod'
) summary
ORDER BY sort_order;

-- Commit transaction
COMMIT;

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRATION COMPLETED SUCCESSFULLY!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'HOD is now a committee-level entity.';
    RAISE NOTICE 'HOD will now appear in the Committee Member dropdown.';
    RAISE NOTICE '========================================';
END $$;
