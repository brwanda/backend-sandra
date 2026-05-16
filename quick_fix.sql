-- ============================================================================
-- QUICK FIX SCRIPT - Run this immediately to fix all issues
-- ============================================================================
-- This script will:
-- 1. Add the missing commissioner_general column
-- 2. Show you what needs to be deleted
-- 3. Show you ngogasandra6@gmail.com's current status
-- ============================================================================

-- Step 1: Add commissioner_general column
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'country_committee_member' 
        AND column_name = 'commissioner_general'
    ) THEN
        ALTER TABLE country_committee_member 
        ADD COLUMN commissioner_general BOOLEAN DEFAULT false;
        RAISE NOTICE '✅ Added commissioner_general column';
    ELSE
        RAISE NOTICE 'ℹ️ commissioner_general column already exists';
    END IF;
END $$;

-- Step 2: Show current state of all members
\echo '\n========== CURRENT MEMBERS =========='
SELECT 
    id,
    name,
    email,
    chair,
    committee_member,
    (SELECT c.name FROM committee c WHERE c.id = committee_id) as committee_name,
    (SELECT co.name FROM country co WHERE co.id = country_id) as country_name
FROM country_committee_member
ORDER BY id;

-- Step 3: Find members that should be deleted
\echo '\n========== MEMBERS TO DELETE =========='
SELECT 
    id,
    name,
    email,
    '⚠️ SHOULD BE DELETED' as status
FROM country_committee_member
WHERE email LIKE '%leader%' 
   OR name LIKE '%Leader%'
   OR email = 'leader12@gmail.com';

-- Step 4: Check HOD committee
\echo '\n========== HOD COMMITTEE =========='
SELECT 
    id,
    name,
    '✅ This is the HOD committee' as note
FROM committee
WHERE LOWER(name) LIKE '%head of delegation%';

-- Step 5: Check ngogasandra6@gmail.com status
\echo '\n========== ngogasandra6@gmail.com STATUS =========='
SELECT 
    ccm.id,
    ccm.name,
    ccm.email,
    ccm.chair,
    c.id as committee_id,
    c.name as committee_name,
    co.name as country_name,
    CASE 
        WHEN ccm.chair = true AND LOWER(c.name) LIKE '%head of delegation%' THEN '✅ HOD (Correct)'
        WHEN ccm.chair = true THEN '⚠️ Chair but NOT in HOD committee'
        ELSE '❌ Not a chair'
    END as status
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
LEFT JOIN country co ON ccm.country_id = co.id
WHERE ccm.email = 'ngogasandra6@gmail.com';

-- Step 6: Show all HOD members
\echo '\n========== ALL HOD MEMBERS =========='
SELECT 
    ccm.id,
    ccm.name,
    ccm.email,
    ccm.chair,
    co.name as country_name,
    CASE 
        WHEN ccm.chair = true THEN '✅ Chair (HOD)'
        ELSE '⚠️ Member (not HOD)'
    END as role
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
LEFT JOIN country co ON ccm.country_id = co.id
WHERE LOWER(c.name) LIKE '%head of delegation%'
ORDER BY ccm.chair DESC, co.name;

-- ============================================================================
-- MANUAL ACTIONS REQUIRED (uncomment and run after reviewing above)
-- ============================================================================

-- Action 1: Delete unwanted members (uncomment to run)
-- DELETE FROM country_committee_member WHERE email = 'leader12@gmail.com';
-- DELETE FROM users WHERE email = 'leader12@gmail.com';

-- Action 2: Fix ngogasandra6@gmail.com (uncomment and replace <HOD_COMMITTEE_ID>)
-- First, get the HOD committee ID from the output above, then:
-- UPDATE country_committee_member 
-- SET committee_id = <HOD_COMMITTEE_ID>, 
--     chair = true,
--     committee_member = false
-- WHERE email = 'ngogasandra6@gmail.com';

-- UPDATE users 
-- SET role = 'HOD'
-- WHERE email = 'ngogasandra6@gmail.com';

-- Action 3: Verify fixes
-- SELECT * FROM country_committee_member WHERE email = 'ngogasandra6@gmail.com';
-- SELECT * FROM country_committee_member WHERE email = 'leader12@gmail.com';

\echo '\n========== NEXT STEPS =========='
\echo '1. Review the output above'
\echo '2. Uncomment and run the DELETE statements for unwanted members'
\echo '3. Uncomment and run the UPDATE statements for ngogasandra6@gmail.com'
\echo '4. Rebuild backend: mvn clean package -DskipTests'
\echo '5. Restart backend server'
\echo '6. Clear browser cache and reload'
