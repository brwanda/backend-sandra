-- Verification Script: Chair of Head of Delegation Setup
-- Run this to check if your Chair of Head of Delegation is properly configured

-- 1. Check if Head of Delegation subcommittee exists
SELECT 
    'Head of Delegation Subcommittee Check' as check_type,
    id, 
    subcommittee_name as name
FROM sub_committee 
WHERE subcommittee_name = 'Head Of Delegation';

-- 2. Check all Chair users and their subcommittees
SELECT 
    'Chair Users Check' as check_type,
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.subcommittee_name as subcommittee_name,
    CASE 
        WHEN sc.subcommittee_name = 'Head Of Delegation' THEN 'YES - HOD PRIVILEGES'
        ELSE 'NO - Regular Chair'
    END as hod_privileges
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'CHAIR';

-- 3. Check all Vice Chair users and their subcommittees
SELECT 
    'Vice Chair Users Check' as check_type,
    u.id,
    u.name,
    u.email,
    u.role,
    u.subcommittee_id,
    sc.subcommittee_name as subcommittee_name,
    CASE 
        WHEN sc.subcommittee_name = 'Head Of Delegation' THEN 'YES - HOD PRIVILEGES'
        ELSE 'NO - Regular Vice Chair'
    END as hod_privileges
FROM users u
LEFT JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role = 'VICE_CHAIR';

-- 4. Check if there are any users with NULL subcommittee_id who should have one
SELECT 
    'Users with NULL subcommittee_id' as check_type,
    id,
    name,
    email,
    role
FROM users 
WHERE role IN ('CHAIR', 'VICE_CHAIR', 'SUBCOMMITTEE_MEMBER') 
AND subcommittee_id IS NULL;

-- 5. Summary: Users who should have HOD privileges
SELECT 
    'HOD Privilege Summary' as check_type,
    COUNT(*) as count,
    'Users with HOD privileges (Chair/Vice Chair of Head of Delegation)' as description
FROM users u
JOIN sub_committee sc ON u.subcommittee_id = sc.id
WHERE u.role IN ('CHAIR', 'VICE_CHAIR') 
AND sc.subcommittee_name = 'Head Of Delegation';

-- 6. If you need to create a Chair of Head of Delegation user, use this template:
/*
INSERT INTO users (name, email, password, role, subcommittee_id, created_at, is_active) 
VALUES (
    'John Doe',  -- Replace with actual name
    'hod.chair@example.com',  -- Replace with actual email
    '$2a$10$...',  -- Replace with hashed password
    'CHAIR',
    (SELECT id FROM sub_committee WHERE subcommittee_name = 'Head Of Delegation'),
    NOW(),
    true
);
*/

-- 7. If you need to update an existing Chair to be Chair of Head of Delegation:
/*
UPDATE users 
SET subcommittee_id = (SELECT id FROM sub_committee WHERE subcommittee_name = 'Head Of Delegation')
WHERE email = 'your.chair@example.com';  -- Replace with actual email
*/
