-- Fix Head of Delegation Subcommittee Assignment Issues
-- This script addresses the problem where members assigned to "Head Of Delegation" 
-- have NULL subcommittee_id in the users table

-- 1. First, let's check the current state
SELECT 'Current state of subcommittees:' as info;
SELECT id, name FROM sub_committee ORDER BY id;

SELECT 'Current state of csub_committee_members with Head of Delegation:' as info;
SELECT 
    csm.id,
    csm.name,
    csm.email,
    csm.chair,
    csm.vice_chair,
    csm.secretary_of_delegation,
    csm.committee_secretary,
    csm.committee_member,
    csm.position_in_ear_id,
    sc.name as subcommittee_name
FROM csub_committee_members csm
LEFT JOIN sub_committee sc ON csm.position_in_ear_id = sc.id
WHERE sc.name = 'Head Of Delegation' OR csm.position_in_ear_id IS NULL;

SELECT 'Current state of users with NULL subcommittee_id:' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.subcommittee_id IS NULL;

-- 2. Fix the issue: Update users table to set subcommittee_id for members in Head of Delegation
UPDATE users 
SET subcommittee_id = (
    SELECT sc.id 
    FROM sub_committee sc 
    WHERE sc.name = 'Head Of Delegation'
    LIMIT 1
)
WHERE users.id IN (
    SELECT DISTINCT u.id
    FROM users u
    INNER JOIN csub_committee_members csm ON u.email = csm.email
    INNER JOIN sub_committee sc ON csm.position_in_ear_id = sc.id
    WHERE sc.name = 'Head Of Delegation'
    AND u.subcommittee_id IS NULL
);

-- 3. Also fix any users who have roles but no subcommittee assignment
UPDATE users 
SET subcommittee_id = (
    SELECT sc.id 
    FROM sub_committee sc 
    WHERE sc.name = 'Head Of Delegation'
    LIMIT 1
)
WHERE users.role IN ('CHAIR', 'VICE_CHAIR', 'DELEGATION_SECRETARY', 'COMMITTEE_SECRETARY', 'COMMITTEE_MEMBER')
AND users.subcommittee_id IS NULL
AND users.email IN (
    SELECT DISTINCT csm.email
    FROM csub_committee_members csm
    INNER JOIN sub_committee sc ON csm.position_in_ear_id = sc.id
    WHERE sc.name = 'Head Of Delegation'
);

-- 4. Verify the fix
SELECT 'After fix - users with Head of Delegation subcommittee:' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE sc.name = 'Head Of Delegation';

SELECT 'Remaining users with NULL subcommittee_id:' as info;
SELECT 
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id
FROM users u
WHERE u.subcommittee_id IS NULL;

-- 5. Create a view to monitor subcommittee assignments
CREATE OR REPLACE VIEW subcommittee_assignment_status AS
SELECT 
    sc.id as subcommittee_id,
    sc.name as subcommittee_name,
    COUNT(u.id) as user_count,
    COUNT(CASE WHEN u.role = 'CHAIR' THEN 1 END) as chair_count,
    COUNT(CASE WHEN u.role = 'VICE_CHAIR' THEN 1 END) as vice_chair_count,
    COUNT(CASE WHEN u.role = 'DELEGATION_SECRETARY' THEN 1 END) as delegation_secretary_count,
    COUNT(CASE WHEN u.role = 'COMMITTEE_SECRETARY' THEN 1 END) as committee_secretary_count,
    COUNT(CASE WHEN u.role = 'COMMITTEE_MEMBER' THEN 1 END) as committee_member_count
FROM sub_committee sc
LEFT JOIN users u ON sc.id = u.subcommittee_id
GROUP BY sc.id, sc.name
ORDER BY sc.id;

-- 6. Show the final status
SELECT 'Final subcommittee assignment status:' as info;
SELECT * FROM subcommittee_assignment_status;
