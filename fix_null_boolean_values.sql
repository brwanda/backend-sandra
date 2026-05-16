-- Fix NULL boolean values in country_committee_member table
-- This fixes the "Null value was assigned to a property" error

BEGIN;

-- Update NULL values to FALSE for all boolean columns
UPDATE country_committee_member
SET 
    chair = COALESCE(chair, false),
    vice_chair = COALESCE(vice_chair, false),
    committee_secretary = COALESCE(committee_secretary, false),
    committee_member = COALESCE(committee_member, false),
    delegation_secretary = COALESCE(delegation_secretary, false)
WHERE 
    chair IS NULL 
    OR vice_chair IS NULL 
    OR committee_secretary IS NULL 
    OR committee_member IS NULL 
    OR delegation_secretary IS NULL;

-- Set NOT NULL constraints to prevent future NULL values
ALTER TABLE country_committee_member 
    ALTER COLUMN chair SET DEFAULT false,
    ALTER COLUMN vice_chair SET DEFAULT false,
    ALTER COLUMN committee_secretary SET DEFAULT false,
    ALTER COLUMN committee_member SET DEFAULT false,
    ALTER COLUMN delegation_secretary SET DEFAULT false;

ALTER TABLE country_committee_member 
    ALTER COLUMN chair SET NOT NULL,
    ALTER COLUMN vice_chair SET NOT NULL,
    ALTER COLUMN committee_secretary SET NOT NULL,
    ALTER COLUMN committee_member SET NOT NULL,
    ALTER COLUMN delegation_secretary SET NOT NULL;

-- Verify the fix
SELECT 'Fixed records:' as info, COUNT(*) as count FROM country_committee_member;
SELECT 'Records with NULL booleans (should be 0):' as info, COUNT(*) as count 
FROM country_committee_member 
WHERE chair IS NULL OR vice_chair IS NULL OR committee_secretary IS NULL 
   OR committee_member IS NULL OR delegation_secretary IS NULL;

COMMIT;

SELECT '✅ NULL boolean values fixed successfully!' as status;
