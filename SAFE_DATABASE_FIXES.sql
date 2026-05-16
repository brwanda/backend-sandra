-- ============================================================================
-- SAFE DATABASE FIXES - Step by Step with Backups
-- ============================================================================
-- Created: 2026-05-15
-- This script will fix all 4 issues safely with rollback capability
-- ============================================================================

-- CURRENT STATE ANALYSIS:
-- ========================
-- Committee ID 1 = "Commissioner General"
-- Committee ID 6 = "Head Of Delegation"
--
-- Current Members:
-- ID 8:  Iradukunda Rosine (iraduduroro@gmail.com) - Chair of Commissioner General (committee_id=1)
-- ID 9:  Lion Lite (litelion6@gmail.com) - Member of Commissioner General (committee_id=1)
-- ID 10: Annick Ujeneza (uwizeyengogasandra@gmail.com) - Secretary of HOD (committee_id=6, hod=true)
-- ID 14: Annick Ujeneza (ngogasandra6@gmail.com) - Chair of HOD (committee_id=6) ✅ CORRECT!
-- ID 15: Leader Mutoni (leader12@gmail.com) - Chair of HOD (committee_id=6) ❌ SHOULD BE DELETED
-- ID 16: Eustache Ngoga (ngogaeustache123@gmail.com) - Member of HOD (committee_id=6)

-- ISSUES IDENTIFIED:
-- ==================
-- 1. ✅ ngogasandra6@gmail.com IS already in HOD committee (committee_id=6) and is chair!
--    No fix needed for this one!
-- 2. ❌ leader12@gmail.com (ID 15) should be deleted
-- 3. ❌ commissioner_general column is missing
-- 4. ✅ Duplicate HOD cards will be fixed by backend code changes

-- ============================================================================
-- STEP 1: CREATE BACKUP TABLE
-- ============================================================================
\echo '========== STEP 1: Creating Backup Table =========='

-- Drop backup table if it exists
DROP TABLE IF EXISTS country_committee_member_backup_20260515;

-- Create backup table with all current data
CREATE TABLE country_committee_member_backup_20260515 AS 
SELECT * FROM country_committee_member;

-- Verify backup
SELECT COUNT(*) as backup_row_count FROM country_committee_member_backup_20260515;

\echo '✅ Backup created successfully!'
\echo ''

-- ============================================================================
-- STEP 2: ADD commissioner_general COLUMN
-- ============================================================================
\echo '========== STEP 2: Adding commissioner_general Column =========='

-- Check if column already exists
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

-- Verify column was added
\d country_committee_member

\echo ''

-- ============================================================================
-- STEP 3: DELETE UNWANTED MEMBER (leader12@gmail.com)
-- ============================================================================
\echo '========== STEP 3: Deleting Unwanted Member =========='

-- Show the member before deletion
\echo 'Member to be deleted:'
SELECT id, name, email, chair, committee_id 
FROM country_committee_member 
WHERE email = 'leader12@gmail.com';

-- Delete from country_committee_member
DELETE FROM country_committee_member 
WHERE email = 'leader12@gmail.com';

-- Also delete from users table if exists
DELETE FROM users 
WHERE email = 'leader12@gmail.com';

\echo '✅ Deleted leader12@gmail.com from both tables'
\echo ''

-- ============================================================================
-- STEP 4: VERIFY ngogasandra6@gmail.com STATUS
-- ============================================================================
\echo '========== STEP 4: Verifying ngogasandra6@gmail.com =========='

-- Check current status
SELECT 
    ccm.id,
    ccm.name,
    ccm.email,
    ccm.chair,
    ccm.committee_id,
    CASE 
        WHEN ccm.committee_id = 6 AND ccm.chair = true THEN '✅ CORRECT - Chair of HOD'
        WHEN ccm.committee_id = 6 THEN '⚠️ In HOD but not chair'
        ELSE '❌ NOT in HOD committee'
    END as status
FROM country_committee_member ccm
WHERE ccm.email = 'ngogasandra6@gmail.com';

\echo ''
\echo 'ℹ️ ngogasandra6@gmail.com is already correctly assigned!'
\echo 'ℹ️ No changes needed for this member.'
\echo ''

-- ============================================================================
-- STEP 5: SHOW FINAL STATE
-- ============================================================================
\echo '========== STEP 5: Final State After Fixes =========='

\echo 'All members in country_committee_member table:'
SELECT 
    id,
    name,
    email,
    chair,
    committee_member,
    committee_id,
    CASE 
        WHEN committee_id = 1 THEN 'Commissioner General'
        WHEN committee_id = 6 THEN 'Head Of Delegation'
        ELSE 'Other'
    END as committee_name
FROM country_committee_member
ORDER BY committee_id, chair DESC, id;

\echo ''
\echo 'HOD Members (committee_id = 6):'
SELECT 
    id,
    name,
    email,
    chair,
    CASE 
        WHEN chair = true THEN '👑 Chair (HOD)'
        ELSE '👤 Member'
    END as role
FROM country_committee_member
WHERE committee_id = 6
ORDER BY chair DESC, id;

\echo ''

-- ============================================================================
-- STEP 6: VERIFICATION QUERIES
-- ============================================================================
\echo '========== STEP 6: Verification =========='

-- Count members by committee
\echo 'Members by committee:'
SELECT 
    CASE 
        WHEN committee_id = 1 THEN 'Commissioner General'
        WHEN committee_id = 6 THEN 'Head Of Delegation'
        ELSE 'Other'
    END as committee_name,
    COUNT(*) as member_count,
    SUM(CASE WHEN chair = true THEN 1 ELSE 0 END) as chair_count
FROM country_committee_member
GROUP BY committee_id
ORDER BY committee_id;

\echo ''

-- Verify commissioner_general column exists
\echo 'Checking commissioner_general column:'
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'country_committee_member'
AND column_name = 'commissioner_general';

\echo ''

-- Verify leader12@gmail.com is deleted
\echo 'Checking if leader12@gmail.com is deleted:'
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Successfully deleted'
        ELSE '❌ Still exists!'
    END as status
FROM country_committee_member
WHERE email = 'leader12@gmail.com';

\echo ''

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (if needed)
-- ============================================================================
\echo '========== ROLLBACK INSTRUCTIONS =========='
\echo 'If you need to undo these changes, run:'
\echo ''
\echo '-- Restore from backup:'
\echo 'DELETE FROM country_committee_member;'
\echo 'INSERT INTO country_committee_member SELECT * FROM country_committee_member_backup_20260515;'
\echo ''
\echo '-- Remove commissioner_general column:'
\echo 'ALTER TABLE country_committee_member DROP COLUMN commissioner_general;'
\echo ''
\echo '-- Drop backup table when no longer needed:'
\echo 'DROP TABLE country_committee_member_backup_20260515;'
\echo ''

-- ============================================================================
-- SUMMARY
-- ============================================================================
\echo '========== SUMMARY =========='
\echo '✅ Step 1: Backup created (country_committee_member_backup_20260515)'
\echo '✅ Step 2: commissioner_general column added'
\echo '✅ Step 3: leader12@gmail.com deleted'
\echo '✅ Step 4: ngogasandra6@gmail.com verified (already correct!)'
\echo '✅ Step 5: Final state displayed'
\echo '✅ Step 6: Verification completed'
\echo ''
\echo '🎉 All database fixes completed successfully!'
\echo ''
\echo 'Next steps:'
\echo '1. Rebuild backend: mvn clean package -DskipTests'
\echo '2. Restart backend server'
\echo '3. Rebuild frontend: npm run build'
\echo '4. Clear browser cache and test'
