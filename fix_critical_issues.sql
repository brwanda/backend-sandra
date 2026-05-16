-- Critical fixes for user_role and subcommittee_id issues
-- Run this script to fix the immediate problems

-- 1. FIRST: Fix all NULL user_role values in csub_committee_members table
UPDATE csub_committee_members 
SET user_role = CASE 
    WHEN chair = TRUE THEN 'CHAIR'
    WHEN vice_chair = TRUE THEN 'VICE_CHAIR'
    WHEN committee_secretary = TRUE THEN 'COMMITTEE_SECRETARY'
    WHEN secretary_of_delegation = TRUE THEN 'DELEGATION_SECRETARY'
    WHEN committee_member = TRUE THEN 'COMMITTEE_MEMBER'
    ELSE 'SUBCOMMITTEE_MEMBER'
END
WHERE user_role IS NULL;

-- 2. THEN: Make user_role column NOT NULL (this will prevent future NULL values)
ALTER TABLE csub_committee_members ALTER COLUMN user_role SET NOT NULL;

-- 3. Verify the fix worked
SELECT 'user_role_null_count' as check_name, COUNT(*) as result 
FROM csub_committee_members 
WHERE user_role IS NULL;
-- This should return 0

-- 4. Check current subcommittee_id issues in users table
SELECT 'users_missing_subcommittee' as check_name, 
       role, 
       COUNT(*) as count,
       string_agg(email, ', ') as affected_users
FROM users 
WHERE role IN ('CHAIR', 'SUBCOMMITTEE_MEMBER') 
  AND subcommittee_id IS NULL
GROUP BY role;

-- 5. Show available subcommittees for reference
SELECT 'available_subcommittees' as info, id, subcommittee_name as name 
FROM sub_committee 
ORDER BY id;

-- 6. Show users with missing country_id for secretary roles
SELECT 'users_missing_country' as check_name, 
       role, 
       COUNT(*) as count,
       string_agg(email, ', ') as affected_users
FROM users 
WHERE role IN ('SECRETARY', 'COMMITTEE_SECRETARY', 'DELEGATION_SECRETARY') 
  AND country_id IS NULL
GROUP BY role;

-- 7. Show available countries for reference
SELECT 'available_countries' as info, id, name 
FROM country 
ORDER BY id;

-- MANUAL FIXES NEEDED:
-- After running this script, you'll need to manually update users with missing subcommittee_id and country_id
-- Example commands (replace with actual IDs and emails):

-- Fix chairs/subcommittee members without subcommittee:
-- UPDATE users SET subcommittee_id = 1 WHERE email = 'chair@example.com' AND role IN ('CHAIR', 'SUBCOMMITTEE_MEMBER');

-- Fix secretaries without country:
-- UPDATE users SET country_id = 1 WHERE email = 'secretary@example.com' AND role IN ('SECRETARY', 'COMMITTEE_SECRETARY', 'DELEGATION_SECRETARY');

-- VERIFICATION QUERIES:
-- Run these after manual fixes to ensure everything is correct:

-- Check user_role is never null:
-- SELECT COUNT(*) FROM csub_committee_members WHERE user_role IS NULL; -- Should be 0

-- Check role requirements are met:
-- SELECT role, COUNT(*) FROM users WHERE 
--   (role IN ('SECRETARY', 'COMMITTEE_SECRETARY', 'DELEGATION_SECRETARY') AND country_id IS NULL) OR
--   (role IN ('CHAIR', 'SUBCOMMITTEE_MEMBER') AND subcommittee_id IS NULL)
-- GROUP BY role; -- Should return no rows
