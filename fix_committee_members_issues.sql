-- ============================================================================
-- FIX COMMITTEE MEMBERS ISSUES
-- ============================================================================
-- This script fixes:
-- 1. Adds missing commissioner_general column
-- 2. Removes deleted members that are still in the database
-- 3. Ensures proper HOD committee assignment
-- ============================================================================

-- Step 1: Add commissioner_general column if it doesn't exist
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

-- Step 2: Show current state of the table
SELECT 
    id,
    name,
    email,
    chair,
    committee_member,
    delegation_secretary,
    commissioner_general,
    (SELECT c.name FROM committee c WHERE c.id = committee_id) as committee_name,
    (SELECT co.name FROM country co WHERE co.id = country_id) as country_name
FROM country_committee_member
ORDER BY id;

-- Step 3: Find and show members that should be deleted (like leader12@gmail.com)
-- These are members that appear in the UI but shouldn't be there
SELECT 
    id,
    name,
    email,
    'SHOULD BE DELETED' as status
FROM country_committee_member
WHERE email IN ('leader12@gmail.com', 'leader.mutoni@gmail.com')
   OR name LIKE '%Leader%';

-- Step 4: Check HOD committee assignment
-- Find all committees with "Head of Delegation" in the name
SELECT 
    id,
    name,
    'HOD Committee' as type
FROM committee
WHERE LOWER(name) LIKE '%head of delegation%';

-- Step 5: Check which members are assigned to HOD committee
SELECT 
    ccm.id,
    ccm.name,
    ccm.email,
    ccm.chair,
    c.name as committee_name,
    co.name as country_name
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
LEFT JOIN country co ON ccm.country_id = co.id
WHERE LOWER(c.name) LIKE '%head of delegation%';

-- Step 6: Find ngogasandra6@gmail.com and check their assignment
SELECT 
    ccm.id,
    ccm.name,
    ccm.email,
    ccm.chair,
    ccm.committee_member,
    c.name as committee_name,
    co.name as country_name,
    CASE 
        WHEN ccm.chair = true AND LOWER(c.name) LIKE '%head of delegation%' THEN 'HOD'
        WHEN ccm.chair = true AND LOWER(c.name) LIKE '%commissioner general%' THEN 'COMMISSIONER_GENERAL'
        WHEN ccm.chair = true THEN 'CHAIR'
        ELSE 'MEMBER'
    END as determined_role
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
LEFT JOIN country co ON ccm.country_id = co.id
WHERE ccm.email = 'ngogasandra6@gmail.com';

-- Step 7: Show all members grouped by committee
SELECT 
    c.name as committee_name,
    COUNT(*) as member_count,
    STRING_AGG(ccm.name || ' (' || ccm.email || ')', ', ') as members
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
GROUP BY c.name
ORDER BY c.name;

-- ============================================================================
-- MANUAL FIXES NEEDED (Run these after reviewing the output above)
-- ============================================================================

-- Fix 1: Delete members that should be removed (uncomment and run if needed)
-- DELETE FROM country_committee_member WHERE email = 'leader12@gmail.com';
-- DELETE FROM country_committee_member WHERE email = 'leader.mutoni@gmail.com';

-- Fix 2: If ngogasandra6@gmail.com is not in HOD committee, reassign them
-- First, find the HOD committee ID:
-- SELECT id, name FROM committee WHERE LOWER(name) LIKE '%head of delegation%';
-- Then update the member:
-- UPDATE country_committee_member 
-- SET committee_id = <HOD_COMMITTEE_ID>, chair = true
-- WHERE email = 'ngogasandra6@gmail.com';

-- Fix 3: Ensure only one HOD member per country (if there are duplicates)
-- This query shows duplicates:
SELECT 
    co.name as country_name,
    COUNT(*) as hod_count,
    STRING_AGG(ccm.name || ' (' || ccm.email || ')', ', ') as hod_members
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
LEFT JOIN country co ON ccm.country_id = co.id
WHERE LOWER(c.name) LIKE '%head of delegation%'
  AND ccm.chair = true
GROUP BY co.name
HAVING COUNT(*) > 1;
