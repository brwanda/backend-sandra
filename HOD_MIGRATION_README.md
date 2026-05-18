# HOD Migration Guide

## Problem Description

When creating a new member and selecting **"Committee Member"** as the entry type, only **"Commissioner General"** appears in the committee dropdown. The **"Head of Delegation (HOD)"** should also appear since HOD members are committee-level members, not sub-committee members.

### Root Cause

**Head of Delegation (HOD)** is currently stored in the `sub_committee` table, but it should be in the `committee` table like "Commissioner General". This is why HOD doesn't appear when you select "Committee Member" - the form only shows entries from the `committee` table.

## Solution

Move HOD from the `sub_committee` table to the `committee` table through a safe database migration.

---

## Migration Files

1. **`migrate_hod_to_committee_table_safe.sql`** - The SQL migration script
2. **`run_hod_migration.ps1`** - PowerShell script to run the migration automatically
3. **`HOD_MIGRATION_README.md`** - This guide

---

## How to Run the Migration

### Option 1: Using PowerShell Script (Recommended)

1. **Open PowerShell** in the `EARACONNECT-BACKEND-DEPLOY4` directory

2. **Run the migration script:**
   ```powershell
   .\run_hod_migration.ps1
   ```

3. **Follow the prompts:**
   - The script will show you what changes will be made
   - Type `yes` to confirm and proceed
   - The migration will run automatically

4. **Verify the results:**
   - The script will show a summary of the migration
   - Check that all steps completed successfully

### Option 2: Using pgAdmin or Database Client

1. **Open pgAdmin** or your preferred PostgreSQL client

2. **Connect to your database:**
   - Database: `eara_connect_db-final-EARA10`
   - Username: `postgres`
   - Password: `123`

3. **Open the SQL file:**
   - Open `migrate_hod_to_committee_table_safe.sql`

4. **Execute the script:**
   - Run the entire script
   - Check the output for any errors

5. **Verify the results:**
   - Check the migration summary at the end

### Option 3: Using Command Line

```bash
# Set password environment variable
export PGPASSWORD=123

# Run the migration
psql -h localhost -p 5432 -U postgres -d eara_connect_db-final-EARA10 -f migrate_hod_to_committee_table_safe.sql

# Clear password
unset PGPASSWORD
```

---

## What the Migration Does

### Step 1: Pre-Migration Checks
- ✅ Checks current HOD in sub_committee table
- ✅ Lists all current committees
- ✅ Shows HOD members in csub_committee_members
- ✅ Shows HOD users

### Step 2: Create HOD Committee
- ✅ Creates "Head Of Delegation" entry in the committee table
- ✅ Verifies the committee was created

### Step 3: Migrate HOD Members
- ✅ Moves all HOD members from `csub_committee_members` to `country_committee_member`
- ✅ Preserves all member data (name, email, phone, roles, etc.)
- ✅ Prevents duplicate entries

### Step 4: Update Users Table
- ✅ Removes `subcommittee_id` from HOD users (they're now committee-level)
- ✅ Verifies users were updated correctly

### Step 5: Cleanup
- ✅ Removes HOD members from `csub_committee_members` (they're now in `country_committee_member`)
- ✅ Removes HOD from `sub_committee` table (it's now in `committee` table)
- ✅ Verifies cleanup was successful

### Step 6: Post-Migration Verification
- ✅ Confirms HOD committee exists
- ✅ Confirms HOD members were migrated
- ✅ Confirms HOD users are correctly configured
- ✅ Shows migration summary

---

## Safety Features

✅ **Transaction-based** - All changes are wrapped in a transaction (can rollback on error)

✅ **Idempotent** - Safe to run multiple times (checks for existing data before inserting)

✅ **Data preservation** - All member data and relationships are preserved

✅ **Detailed logging** - Shows progress at each step

✅ **Verification** - Checks results after each step

---

## After Migration

### 1. Restart Backend (if running)

If your Spring Boot backend is running, restart it:

```bash
# Stop the backend (Ctrl+C if running in terminal)
# Or kill the process on port 8081

# Start the backend again
cd EARACONNECT-BACKEND-DEPLOY4
mvnw spring-boot:run
```

### 2. Refresh Frontend

If your React frontend is running, refresh the browser or restart it:

```bash
# In the browser, press Ctrl+F5 to hard refresh
# Or restart the frontend:
cd EARACONNECT-FRONTEND-DEPLOY4
npm start
```

### 3. Test the Fix

1. Navigate to **Add Member** page (`earaconnect.vercel.app/members/new`)
2. Select **"Committee Member"** as the entry type
3. Select a country
4. Open the **Committee** dropdown
5. **Verify:** You should now see both:
   - ✅ Commissioner General
   - ✅ Head Of Delegation

---

## Rollback (If Needed)

If something goes wrong, you can rollback by:

1. **Restore from backup** (if you have one)

2. **Or manually reverse the changes:**

```sql
BEGIN;

-- Recreate HOD in sub_committee
INSERT INTO sub_committee (subcommittee_name, parent_committee_id)
SELECT 'Head Of Delegation', 1
WHERE NOT EXISTS (
    SELECT 1 FROM sub_committee WHERE subcommittee_name = 'Head Of Delegation'
);

-- Move members back to csub_committee_members
-- (This is complex - better to restore from backup)

-- Delete HOD from committee
DELETE FROM committee WHERE committee_name = 'Head Of Delegation';

COMMIT;
```

**Note:** It's better to have a database backup before running the migration.

---

## Troubleshooting

### Issue: "psql not found"

**Solution:** Install PostgreSQL or add it to your PATH, or use pgAdmin instead.

### Issue: "Permission denied"

**Solution:** Make sure you're using the correct database credentials.

### Issue: "Duplicate key error"

**Solution:** The migration is idempotent. If HOD already exists in the committee table, it will skip the insert. This is normal.

### Issue: "Foreign key constraint violation"

**Solution:** This shouldn't happen with the safe migration script. If it does, check that your database schema matches the expected structure.

---

## Expected Results

### Before Migration:
- HOD is in `sub_committee` table
- HOD members are in `csub_committee_members` table
- Committee dropdown shows only "Commissioner General"

### After Migration:
- HOD is in `committee` table
- HOD members are in `country_committee_member` table
- Committee dropdown shows both "Commissioner General" and "Head Of Delegation"

---

## Support

If you encounter any issues:

1. Check the migration output for error messages
2. Verify your database schema matches the expected structure
3. Check the backend logs for any errors
4. Ensure the backend and frontend are restarted after migration

---

## Summary

This migration safely moves HOD from being a sub-committee to being a committee-level entity, which is the correct structure. After running this migration, HOD will appear in the Committee Member dropdown alongside Commissioner General, fixing the issue you reported.

**Estimated time:** 1-2 minutes

**Risk level:** Low (transaction-based, can rollback)

**Downtime required:** None (but restart backend after migration)
