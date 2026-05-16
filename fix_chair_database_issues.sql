-- Fix Chair Role Database Issues
-- This script addresses the NULL user_role and subcommittee_id issues

-- 1. Fix NULL user_role in csub_committee_members table
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

-- 2. Ensure user_role column is NOT NULL
ALTER TABLE csub_committee_members ALTER COLUMN user_role SET NOT NULL;

-- 3. Fix subcommittee_id in users table for Chair users
-- Update users who have CHAIR role but missing subcommittee_id
UPDATE users 
SET subcommittee_id = (
    SELECT csm.position_in_ear_id 
    FROM csub_committee_members csm 
    WHERE csm.name = users.name 
    AND csm.chair = TRUE 
    LIMIT 1
)
WHERE users.role = 'CHAIR' 
AND users.subcommittee_id IS NULL;

-- 4. Fix subcommittee_id for VICE_CHAIR users
UPDATE users 
SET subcommittee_id = (
    SELECT csm.position_in_ear_id 
    FROM csub_committee_members csm 
    WHERE csm.name = users.name 
    AND csm.vice_chair = TRUE 
    LIMIT 1
)
WHERE users.role = 'VICE_CHAIR' 
AND users.subcommittee_id IS NULL;

-- 5. Fix subcommittee_id for SUBCOMMITTEE_MEMBER users
UPDATE users 
SET subcommittee_id = (
    SELECT csm.position_in_ear_id 
    FROM csub_committee_members csm 
    WHERE csm.name = users.name 
    AND csm.committee_member = TRUE 
    LIMIT 1
)
WHERE users.role = 'SUBCOMMITTEE_MEMBER' 
AND users.subcommittee_id IS NULL;

-- 6. Verify the fixes
SELECT 'csub_committee_members with NULL user_role:' as check_type, COUNT(*) as count
FROM csub_committee_members 
WHERE user_role IS NULL
UNION ALL
SELECT 'users with CHAIR role but NULL subcommittee_id:' as check_type, COUNT(*) as count
FROM users 
WHERE role = 'CHAIR' AND subcommittee_id IS NULL
UNION ALL
SELECT 'users with VICE_CHAIR role but NULL subcommittee_id:' as check_type, COUNT(*) as count
FROM users 
WHERE role = 'VICE_CHAIR' AND subcommittee_id IS NULL
UNION ALL
SELECT 'users with SUBCOMMITTEE_MEMBER role but NULL subcommittee_id:' as check_type, COUNT(*) as count
FROM users 
WHERE role = 'SUBCOMMITTEE_MEMBER' AND subcommittee_id IS NULL;
