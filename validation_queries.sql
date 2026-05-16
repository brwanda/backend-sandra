-- Validation queries to test database fixes
-- Run these queries to verify that the fixes are working correctly

-- 1. Check for NULL user_role in csub_committee_members (should return 0)
SELECT 'NULL user_role count' as test_name, COUNT(*) as result 
FROM csub_committee_members 
WHERE user_role IS NULL;

-- 2. Check users with missing required fields by role
SELECT 
    'Users missing required fields' as test_name,
    role, 
    COUNT(*) as total_users,
    COUNT(CASE WHEN country_id IS NULL THEN 1 END) as missing_country,
    COUNT(CASE WHEN subcommittee_id IS NULL THEN 1 END) as missing_subcommittee,
    COUNT(CASE WHEN phone IS NULL OR phone = '' THEN 1 END) as missing_phone
FROM users 
GROUP BY role
ORDER BY role;

-- 3. Check secretaries without country_id (should be 0 or minimal)
SELECT 'Secretaries without country' as test_name, COUNT(*) as result
FROM users 
WHERE role IN ('SECRETARY', 'COMMITTEE_SECRETARY', 'DELEGATION_SECRETARY') 
  AND country_id IS NULL;

-- 4. Check chairs/subcommittee members without subcommittee_id (should be 0 or minimal)
SELECT 'Chairs/Members without subcommittee' as test_name, COUNT(*) as result
FROM users 
WHERE role IN ('CHAIR', 'SUBCOMMITTEE_MEMBER') 
  AND subcommittee_id IS NULL;

-- 5. Check login tracking fields
SELECT 
    'Login tracking status' as test_name,
    COUNT(*) as total_users,
    COUNT(CASE WHEN is_first_login IS NULL THEN 1 END) as null_first_login,
    COUNT(CASE WHEN is_first_login = TRUE THEN 1 END) as first_login_true,
    COUNT(CASE WHEN last_login IS NULL THEN 1 END) as never_logged_in,
    COUNT(CASE WHEN password_reset_required IS NULL THEN 1 END) as null_password_reset
FROM users;

-- 6. Check role consistency between users and csub_committee_members
SELECT 
    'Role consistency check' as test_name,
    COUNT(*) as total_matches,
    COUNT(CASE WHEN u.role != cm.user_role THEN 1 END) as role_mismatches
FROM users u
JOIN csub_committee_members cm ON u.email = cm.email
WHERE cm.user_role IS NOT NULL;

-- 7. Check for duplicate emails across tables
SELECT 
    'Duplicate email check' as test_name,
    COUNT(*) as total_emails,
    COUNT(DISTINCT email) as unique_emails,
    COUNT(*) - COUNT(DISTINCT email) as duplicates
FROM (
    SELECT email FROM users
    UNION ALL
    SELECT email FROM csub_committee_members WHERE email IS NOT NULL
) combined_emails;

-- 8. Validate role enum values
SELECT 
    'Invalid role values in users' as test_name,
    COUNT(*) as result
FROM users 
WHERE role NOT IN (
    'ADMIN', 'SECRETARY', 'CHAIR', 'VICE_CHAIR', 'HOD', 
    'COMMISSIONER_GENERAL', 'SUBCOMMITTEE_MEMBER', 
    'DELEGATION_SECRETARY', 'COMMITTEE_SECRETARY', 'COMMITTEE_MEMBER'
);

-- 9. Check committee members role assignment logic
SELECT 
    'Committee members role validation' as test_name,
    COUNT(*) as total_members,
    COUNT(CASE WHEN NOT (chair OR vice_chair OR secretary_of_delegation OR committee_secretary OR committee_member) THEN 1 END) as no_roles_assigned,
    COUNT(CASE WHEN (CAST(chair AS INT) + CAST(vice_chair AS INT) + CAST(secretary_of_delegation AS INT) + CAST(committee_secretary AS INT) + CAST(committee_member AS INT)) > 2 THEN 1 END) as too_many_roles
FROM csub_committee_members;

-- 10. Check for orphaned records (committee members without corresponding users)
SELECT 
    'Orphaned committee members' as test_name,
    COUNT(*) as result
FROM csub_committee_members cm
LEFT JOIN users u ON cm.email = u.email
WHERE u.email IS NULL AND cm.email IS NOT NULL;

-- 11. Performance check - ensure indexes exist
SELECT 
    'Index check' as test_name,
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename IN ('users', 'csub_committee_members')
ORDER BY tablename, indexname;

-- 12. Data completeness summary
SELECT 
    'Data completeness summary' as test_name,
    'users' as table_name,
    COUNT(*) as total_records,
    ROUND(100.0 * COUNT(CASE WHEN name IS NOT NULL AND name != '' THEN 1 END) / COUNT(*), 2) as name_completeness,
    ROUND(100.0 * COUNT(CASE WHEN email IS NOT NULL AND email != '' THEN 1 END) / COUNT(*), 2) as email_completeness,
    ROUND(100.0 * COUNT(CASE WHEN phone IS NOT NULL AND phone != '' THEN 1 END) / COUNT(*), 2) as phone_completeness,
    ROUND(100.0 * COUNT(country_id) / COUNT(*), 2) as country_completeness,
    ROUND(100.0 * COUNT(subcommittee_id) / COUNT(*), 2) as subcommittee_completeness
FROM users

UNION ALL

SELECT 
    'Data completeness summary' as test_name,
    'csub_committee_members' as table_name,
    COUNT(*) as total_records,
    ROUND(100.0 * COUNT(CASE WHEN name IS NOT NULL AND name != '' THEN 1 END) / COUNT(*), 2) as name_completeness,
    ROUND(100.0 * COUNT(CASE WHEN email IS NOT NULL AND email != '' THEN 1 END) / COUNT(*), 2) as email_completeness,
    ROUND(100.0 * COUNT(CASE WHEN phone IS NOT NULL AND phone != '' THEN 1 END) / COUNT(*), 2) as phone_completeness,
    ROUND(100.0 * COUNT(country_id) / COUNT(*), 2) as country_completeness,
    ROUND(100.0 * COUNT(user_role) / COUNT(*), 2) as user_role_completeness
FROM csub_committee_members;

-- Expected results after fixes:
-- 1. NULL user_role count should be 0
-- 2. Missing required fields should be minimal and documented
-- 3. Secretaries without country should be 0
-- 4. Chairs/Members without subcommittee should be 0 or documented exceptions
-- 5. Login tracking fields should be properly initialized
-- 6. Role consistency should show 0 mismatches
-- 7. Duplicate emails should be 0 or documented
-- 8. Invalid role values should be 0
-- 9. Committee members should have proper role assignments
-- 10. Orphaned committee members should be 0 or documented
-- 11. Indexes should exist for performance
-- 12. Data completeness should show high percentages for critical fields
