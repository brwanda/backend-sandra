-- Update the users table role constraint to allow new role values
-- First, drop the existing constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add the new constraint with all the role values
ALTER TABLE users ADD CONSTRAINT users_role_check 
CHECK (role IN (
    'ADMIN',
    'SECRETARY', 
    'CHAIR',
    'VICE_CHAIR',
    'HOD',
    'COMMISSIONER_GENERAL',
    'SUBCOMMITTEE_MEMBER',
    'DELEGATION_SECRETARY',
    'COMMITTEE_SECRETARY',
    'COMMITTEE_MEMBER'
));

-- Verify the constraint was added
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'users_role_check'; 