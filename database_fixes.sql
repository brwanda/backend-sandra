-- Database fixes for committee management system
-- Run these queries to fix the identified issues

-- 1. Add missing columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_first_login BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_required BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS position VARCHAR(255);

-- 2. Update existing users to have proper default values for new fields
UPDATE users SET is_first_login = TRUE WHERE is_first_login IS NULL;
UPDATE users SET password_reset_required = FALSE WHERE password_reset_required IS NULL;

-- 3. Fix NULL user_role in csub_committee_members table
-- This will be handled by the application logic, but we can set some defaults
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

-- 3a. Make user_role column NOT NULL after fixing NULL values
ALTER TABLE csub_committee_members ALTER COLUMN user_role SET NOT NULL;

-- 4. Update users table with missing country_id for secretaries
-- This requires manual intervention based on business rules
-- Example for updating a specific secretary:
-- UPDATE users SET country_id = 1 WHERE role = 'SECRETARY' AND email = 'secretary@example.com';

-- 5. Update users table with missing subcommittee_id for chairs and subcommittee members
-- This also requires manual intervention based on business rules
-- Example:
-- UPDATE users SET subcommittee_id = 1 WHERE role IN ('CHAIR', 'SUBCOMMITTEE_MEMBER') AND email = 'chair@example.com';

-- 6. Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login);
CREATE INDEX IF NOT EXISTS idx_users_role_country ON users(role, country_id);
CREATE INDEX IF NOT EXISTS idx_users_role_subcommittee ON users(role, subcommittee_id);
CREATE INDEX IF NOT EXISTS idx_csub_committee_members_user_role ON csub_committee_members(user_role);

-- 7. Add constraints to ensure data integrity
-- Make sure phone numbers follow a pattern (optional)
-- ALTER TABLE users ADD CONSTRAINT check_phone_format CHECK (phone IS NULL OR phone ~ '^[+]?[0-9\s\-\(\)]+$');

-- 8. Create a view for easier committee member role analysis
CREATE OR REPLACE VIEW committee_member_roles AS
SELECT 
    id,
    name,
    email,
    phone,
    country_id,
    user_role,
    CASE 
        WHEN chair = TRUE THEN 'Chair'
        WHEN vice_chair = TRUE THEN 'Vice Chair'
        WHEN committee_secretary = TRUE THEN 'Committee Secretary'
        WHEN secretary_of_delegation = TRUE THEN 'Delegation Secretary'
        WHEN committee_member = TRUE THEN 'Committee Member'
        ELSE 'Other'
    END as primary_role,
    chair,
    vice_chair,
    committee_secretary,
    secretary_of_delegation,
    committee_member
FROM csub_committee_members;

-- 9. Verification queries to check data integrity after fixes
-- Run these to verify the fixes worked:

-- Check for NULL user_role in csub_committee_members
-- SELECT COUNT(*) as null_user_roles FROM csub_committee_members WHERE user_role IS NULL;

-- Check users with missing required fields by role
-- SELECT role, COUNT(*) as count, 
--        COUNT(CASE WHEN country_id IS NULL THEN 1 END) as missing_country,
--        COUNT(CASE WHEN subcommittee_id IS NULL THEN 1 END) as missing_subcommittee,
--        COUNT(CASE WHEN phone IS NULL THEN 1 END) as missing_phone
-- FROM users 
-- GROUP BY role;

-- Check login tracking fields
-- SELECT COUNT(*) as users_with_null_first_login FROM users WHERE is_first_login IS NULL;
-- SELECT COUNT(*) as users_never_logged_in FROM users WHERE last_login IS NULL;
