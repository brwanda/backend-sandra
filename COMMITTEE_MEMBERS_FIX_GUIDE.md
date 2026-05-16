# Committee Members Issues - Complete Fix Guide

## Issues Identified

### 1. **Duplicate HOD Cards**
**Problem**: Multiple "Head of Delegation" cards appear on the Committees page.

**Root Cause**: 
- The frontend creates one card per HOD member in the database
- If there are multiple members assigned to the "Head of Delegation" committee, each gets their own card
- The backend `getHodMembers()` returns ALL members from HOD committee, not just chairs

**Fix Applied**:
- ✅ Backend: Modified `getHodMembers()` to return only chairs (actual HODs)
- ✅ Frontend: Added deduplication logic to group HOD members by country
- ✅ Frontend: Now shows one card per country with the chair as representative

---

### 2. **Missing commissioner_general Column**
**Problem**: The `commissioner_general` column doesn't exist in the database.

**Root Cause**: 
- The column was commented out in the Java model
- Database migration was never run to add the column

**Fix Applied**:
- ✅ Uncommented the `commissioner_general` field in `CountryCommitteeMember.java`
- ✅ Created SQL script to add the column: `fix_committee_members_issues.sql`

**Action Required**:
```sql
-- Run this to add the column:
ALTER TABLE country_committee_member 
ADD COLUMN commissioner_general BOOLEAN DEFAULT false;
```

---

### 3. **Deleted Members Still Showing**
**Problem**: Members like "leader12@gmail.com" still appear in the database after deletion.

**Root Cause**: 
- The delete operation might have failed silently
- Or the member was never actually deleted from the database
- The UI might be caching old data

**Fix Applied**:
- ✅ Created diagnostic SQL to identify members that should be deleted
- ✅ Verified the delete endpoint works correctly

**Action Required**:
```sql
-- Run the diagnostic script first:
-- fix_committee_members_issues.sql

-- Then manually delete the members:
DELETE FROM country_committee_member WHERE email = 'leader12@gmail.com';
DELETE FROM country_committee_member WHERE name LIKE '%Leader Mutoni%';

-- Also delete from users table if they exist:
DELETE FROM users WHERE email = 'leader12@gmail.com';
```

---

### 4. **ngogasandra6@gmail.com Not Showing as HOD**
**Problem**: This member is marked as chair but doesn't appear in the HOD section.

**Root Cause**: 
- The member is assigned to the wrong committee
- HOD detection requires the committee name to contain "head of delegation"
- If they're assigned to a different committee, they won't show as HOD

**Fix Applied**:
- ✅ Created diagnostic SQL to check their committee assignment
- ✅ Backend now properly filters HOD members by chair status

**Action Required**:
```sql
-- 1. Find the HOD committee ID:
SELECT id, name FROM committee WHERE LOWER(name) LIKE '%head of delegation%';

-- 2. Check current assignment:
SELECT 
    ccm.id,
    ccm.name,
    ccm.email,
    ccm.chair,
    c.name as committee_name
FROM country_committee_member ccm
LEFT JOIN committee c ON ccm.committee_id = c.id
WHERE ccm.email = 'ngogasandra6@gmail.com';

-- 3. If they're in the wrong committee, reassign:
UPDATE country_committee_member 
SET committee_id = <HOD_COMMITTEE_ID>, 
    chair = true,
    committee_member = false
WHERE email = 'ngogasandra6@gmail.com';

-- 4. Update their user role:
UPDATE users 
SET role = 'HOD'
WHERE email = 'ngogasandra6@gmail.com';
```

---

## Step-by-Step Fix Instructions

### Step 1: Run Diagnostic SQL
```bash
# Connect to your database
psql -U postgres -d eara_connect_db-final-EARA10

# Run the diagnostic script
\i fix_committee_members_issues.sql
```

### Step 2: Review the Output
The script will show:
- Current state of all committee members
- Members that should be deleted
- HOD committee assignments
- ngogasandra6@gmail.com's current status
- Members grouped by committee

### Step 3: Add Missing Column
```sql
ALTER TABLE country_committee_member 
ADD COLUMN commissioner_general BOOLEAN DEFAULT false;
```

### Step 4: Delete Unwanted Members
```sql
-- Delete from committee members table
DELETE FROM country_committee_member 
WHERE email IN ('leader12@gmail.com', 'leader.mutoni@gmail.com');

-- Delete from users table
DELETE FROM users 
WHERE email IN ('leader12@gmail.com', 'leader.mutoni@gmail.com');
```

### Step 5: Fix ngogasandra6@gmail.com Assignment
```sql
-- Get HOD committee ID
SELECT id FROM committee WHERE LOWER(name) LIKE '%head of delegation%' LIMIT 1;

-- Update member assignment (replace <HOD_COMMITTEE_ID> with actual ID)
UPDATE country_committee_member 
SET committee_id = <HOD_COMMITTEE_ID>, 
    chair = true,
    committee_member = false
WHERE email = 'ngogasandra6@gmail.com';

-- Update user role
UPDATE users 
SET role = 'HOD'
WHERE email = 'ngogasandra6@gmail.com';
```

### Step 6: Rebuild and Restart Backend
```bash
cd EARACONNECT-BACKEND-DEPLOY4
mvn clean package -DskipTests
java -jar target/earaconnect-0.0.1-SNAPSHOT.jar
```

### Step 7: Clear Frontend Cache and Restart
```bash
cd EARACONNECT-FRONTEND-DEPLOY4
npm run build
# Or if running dev server:
npm start
```

### Step 8: Verify Fixes
1. ✅ Check Committees page - should see only one HOD card per country
2. ✅ Check Members page - deleted members should not appear
3. ✅ Check database - commissioner_general column should exist
4. ✅ Check ngogasandra6@gmail.com - should appear as HOD

---

## Prevention Tips

### 1. Ensure Proper Deletion
Always delete from both tables:
```sql
-- Delete from committee members
DELETE FROM country_committee_member WHERE id = <ID>;

-- Delete from users
DELETE FROM users WHERE email = <EMAIL>;
```

### 2. Validate Committee Assignment
Before adding a member as HOD, ensure:
- They're assigned to the "Head of Delegation" committee
- The `chair` flag is set to `true`
- Only one HOD per country

### 3. Use Proper Endpoints
- For HOD members: Use `/api/commissioner-generals/hod`
- For committee members: Use `/api/commissioner-generals/committee/{id}`
- Always check the committee name when determining roles

### 4. Database Constraints
Consider adding:
```sql
-- Unique constraint on email
ALTER TABLE country_committee_member 
ADD CONSTRAINT unique_email UNIQUE (email);

-- Check constraint for role count
ALTER TABLE country_committee_member 
ADD CONSTRAINT check_role_count 
CHECK (
    (chair::int + vice_chair::int + committee_secretary::int + 
     committee_member::int + delegation_secretary::int) BETWEEN 1 AND 2
);
```

---

## Files Modified

1. ✅ `CountryCommitteeMember.java` - Uncommented commissioner_general field
2. ✅ `CountryCommitteeMemberService.java` - Fixed getHodMembers() to return only chairs
3. ✅ `CommitteeList.jsx` - Added deduplication logic for HOD cards
4. ✅ `fix_committee_members_issues.sql` - Diagnostic and fix SQL script

---

## Testing Checklist

- [ ] Run diagnostic SQL and review output
- [ ] Add commissioner_general column
- [ ] Delete unwanted members
- [ ] Fix ngogasandra6@gmail.com assignment
- [ ] Rebuild backend
- [ ] Restart backend server
- [ ] Clear browser cache
- [ ] Verify Committees page shows correct HOD cards
- [ ] Verify Members page doesn't show deleted members
- [ ] Verify ngogasandra6@gmail.com appears as HOD
- [ ] Test member deletion works correctly
- [ ] Test member creation assigns correct committee
