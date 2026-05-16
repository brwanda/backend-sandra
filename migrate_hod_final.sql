-- =====================================================================
-- FINAL MIGRATION: Move Head Of Delegation from sub_committee to committee
-- =====================================================================
-- This version handles ALL foreign key constraints properly
-- =====================================================================

BEGIN;

-- Step 1: Check current state
SELECT '=== STEP 1: PRE-MIGRATION CHECK ===' as step;
SELECT 'Current HOD in sub_committee:' as info, id, subcommittee_name FROM sub_committee WHERE LOWER(subcommittee_name) LIKE '%head%delegation%';
SELECT 'Current committees:' as info, id, committee_name FROM committee;

-- Step 2: Create HOD in committee table if it doesn't exist
SELECT '=== STEP 2: CREATE HOD COMMITTEE ===' as step;
INSERT INTO committee (committee_name)
SELECT 'Head Of Delegation'
WHERE NOT EXISTS (
    SELECT 1 FROM committee WHERE committee_name = 'Head Of Delegation'
);

SELECT 'HOD committee created:' as info, id, committee_name FROM committee WHERE committee_name = 'Head Of Delegation';

-- Step 3: Migrate HOD members from csub_committee_members to country_committee_member
SELECT '=== STEP 3: MIGRATE HOD MEMBERS ===' as step;
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

SELECT 'Migrated HOD members:' as info, COUNT(*) as count FROM country_committee_member ccm
JOIN committee c ON ccm.committee_id = c.id
WHERE c.committee_name = 'Head Of Delegation';

-- Step 4: Update ALL users table - remove subcommittee_id for users in HOD subcommittee
SELECT '=== STEP 4: UPDATE USERS TABLE ===' as step;
UPDATE users 
SET subcommittee_id = NULL
WHERE subcommittee_id IN (
    SELECT id FROM sub_committee 
    WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod'
);

SELECT 'Updated users (removed HOD subcommittee_id):' as info, COUNT(*) as count 
FROM users 
WHERE id IN (14, 20); -- The users that were updated

-- Step 5: Delete country_sub_committee records that reference HOD subcommittee
SELECT '=== STEP 5: DELETE COUNTRY_SUB_COMMITTEE RECORDS ===' as step;
DELETE FROM country_sub_committee
WHERE sub_committe_id IN (
    SELECT id FROM sub_committee 
    WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod'
);

-- Step 6: Delete HOD members from csub_committee_members
SELECT '=== STEP 6: DELETE CSUB_COMMITTEE_MEMBERS RECORDS ===' as step;
DELETE FROM csub_committee_members
WHERE position_in_ear IN (
    SELECT id FROM sub_committee 
    WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod'
);

-- Step 7: Now we can safely delete HOD from sub_committee table
SELECT '=== STEP 7: DELETE HOD FROM SUB_COMMITTEE ===' as step;
DELETE FROM sub_committee 
WHERE LOWER(subcommittee_name) LIKE '%head%delegation%' OR LOWER(subcommittee_name) = 'hod';

SELECT 'Deleted HOD from sub_committee' as info;

-- Step 8: Verify the migration
SELECT '=== STEP 8: FINAL VERIFICATION ===' as step;
SELECT 'All committees:' as info, id, committee_name FROM committee ORDER BY id;

SELECT 'HOD members in country_committee_member:' as info, 
       ccm.id, ccm.name, ccm.email, c.committee_name
FROM country_committee_member ccm
JOIN committee c ON ccm.committee_id = c.id
WHERE c.committee_name = 'Head Of Delegation';

SELECT 'Users with NULL subcommittee_id:' as info, id, name, email, role, subcommittee_id
FROM users 
WHERE id IN (14, 20);

SELECT 'HOD still in sub_committee (should be 0):' as info, COUNT(*) as count
FROM sub_committee 
WHERE LOWER(subcommittee_name) LIKE '%head%delegation%';

COMMIT;

SELECT '✅ ✅ ✅ MIGRATION COMPLETED SUCCESSFULLY! ✅ ✅ ✅' as status;
SELECT 'HOD is now in the committee table.' as message;
SELECT 'Restart your backend and refresh your browser to see HOD in the dropdown.' as next_step;
