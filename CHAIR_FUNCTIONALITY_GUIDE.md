# Chair (Subcommittee) Functionality Guide

## Overview
This guide covers the complete Chair role functionality in the committee management system, including database fixes, API endpoints, and frontend components.

## 🚨 **CRITICAL DATABASE FIXES**

### Issue 1: NULL user_role in csub_committee_members
**Problem**: All records (IDs: 9, 11, 12, 14, 15, 16) have NULL user_role values.

**Solution**: Run the database migration script:
```sql
-- Fix NULL user_role values
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

-- Make column NOT NULL
ALTER TABLE csub_committee_members ALTER COLUMN user_role SET NOT NULL;
```

### Issue 2: Missing subcommittee_id in users table
**Problem**: Users with CHAIR/VICE_CHAIR roles have NULL subcommittee_id.

**Solution**: Run the database migration script:
```sql
-- Fix subcommittee_id for Chair users
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

-- Fix subcommittee_id for Vice Chair users
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
```

## 🎯 **CHAIR FUNCTIONALITIES**

### 1. Report Submission
- **Submit reports** on assigned resolution tasks
- **Include progress details** and hindrances
- **Rate performance** (0-100% completion)
- **Submit multiple reports** for the same resolution
- **Validation**: Progress details required (min 10 chars), performance percentage (0-100)

### 2. Report Management
- **View all submitted reports**
- **Track report status** (SUBMITTED, APPROVED, REJECTED)
- **Update reports** after HOD rejection
- **View HOD comments** on rejected reports

### 3. Resolution Management
- **View assigned resolutions** for their subcommittee
- **Access resolution details** (title, description, deadline, contribution %)
- **Submit reports** for assigned resolutions only

### 4. Profile Management
- **Update personal profile** (name, email, phone)
- **Cannot change role** (set by Admin)
- **View subcommittee assignment**

### 5. Notifications
- **Receive notifications** for:
  - Report rejections with HOD comments
  - New resolution assignments
  - Meeting invitations
  - Task updates

## 🔧 **BACKEND IMPLEMENTATION**

### New Services Created:

#### 1. ChairValidationService.java
```java
@Service
public class ChairValidationService {
    // Validate Chair role
    public boolean isChair(Long userId)
    
    // Validate resolution access
    public boolean canAccessResolution(Long chairId, Long resolutionId)
    
    // Validate report submission
    public boolean canSubmitReport(Long chairId, Long resolutionId)
    
    // Validate report data
    public List<String> validateReportData(Report report)
    
    // Get assigned resolutions
    public List<Resolution> getAssignedResolutions(Long chairId)
    
    // Get Chair reports
    public List<Report> getChairReports(Long chairId)
}
```

#### 2. ChairController.java
```java
@RestController
@RequestMapping("/api/chair")
public class ChairController {
    // GET /api/chair/resolutions/{chairId}
    // POST /api/chair/reports
    // GET /api/chair/reports/{chairId}
    // PUT /api/chair/reports/{reportId}
    // GET /api/chair/profile/{chairId}
    // PUT /api/chair/profile/{chairId}
    // GET /api/chair/subcommittee/{chairId}
    // POST /api/chair/validate
}
```

### API Endpoints:

#### Report Management
```
POST /api/chair/reports?chairId={id}
- Submit new report for resolution
- Validates Chair role and resolution access
- Validates report data (progress, performance %)

GET /api/chair/reports/{chairId}
- Get all reports submitted by Chair
- Returns reports with resolution details

PUT /api/chair/reports/{reportId}?chairId={id}
- Update existing report
- Only allowed for own reports in SUBMITTED/REJECTED status
```

#### Resolution Management
```
GET /api/chair/resolutions/{chairId}
- Get all resolutions assigned to Chair's subcommittee
- Returns resolution details with deadlines and contribution %
```

#### Profile Management
```
GET /api/chair/profile/{chairId}
- Get Chair's profile information
- Includes subcommittee assignment

PUT /api/chair/profile/{chairId}
- Update Chair's profile
- Cannot update role (blocked by validation)
```

#### Validation
```
POST /api/chair/validate
- Validate Chair permissions for specific actions
- Actions: submit_report, access_resolution, update_report
```

## 🎨 **FRONTEND IMPLEMENTATION**

### New Components Created:

#### 1. EnhancedChairDashboard.jsx
- **Modern UI** with gradient background
- **Real-time data** (no hardcoded fallbacks)
- **Three main tabs**:
  - Assigned Resolutions
  - My Reports
  - Notifications
- **Report submission modal** with validation
- **Profile update modal**
- **Responsive design**

#### 2. EnhancedChairDashboard.css
- **Glassmorphism design** with backdrop blur
- **Smooth animations** and hover effects
- **Mobile responsive**
- **Professional color scheme**

### Key Features:
- **No hardcoded data** - all data fetched from API
- **Detailed logging** for debugging
- **Error handling** with user-friendly messages
- **Loading states** with spinners
- **Form validation** with real-time feedback

## 🧪 **TESTING**

### Database Validation Queries:
```sql
-- Check for NULL user_role
SELECT COUNT(*) FROM csub_committee_members WHERE user_role IS NULL;

-- Check for Chair users with NULL subcommittee_id
SELECT COUNT(*) FROM users WHERE role = 'CHAIR' AND subcommittee_id IS NULL;

-- Verify Chair assignments
SELECT u.name, u.role, sc.name as subcommittee_name
FROM users u
LEFT JOIN sub_committees sc ON u.subcommittee_id = sc.id
WHERE u.role IN ('CHAIR', 'VICE_CHAIR');
```

### API Testing:
```bash
# Test Chair validation
curl -X POST http://localhost:8080/api/chair/validate \
  -H "Content-Type: application/json" \
  -d '{"chairId": 1, "action": "submit_report", "resourceId": 1}'

# Test getting assigned resolutions
curl http://localhost:8080/api/chair/resolutions/1

# Test submitting a report
curl -X POST http://localhost:8080/api/chair/reports?chairId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "resolution": {"id": 1},
    "subcommittee": {"id": 1},
    "progressDetails": "Completed initial assessment",
    "hindrances": "Budget approval pending",
    "performancePercentage": 75
  }'
```

### Frontend Testing:
1. **Login as Chair** user
2. **Navigate to Chair Dashboard**
3. **Check assigned resolutions** are loaded
4. **Submit a test report** with validation
5. **Update profile** information
6. **Verify notifications** are working

## 🚀 **DEPLOYMENT STEPS**

### 1. Database Migration
```bash
# Run the database fixes
psql -d your_database -f fix_chair_database_issues.sql
```

### 2. Backend Deployment
```bash
# Build and start the backend
cd eara_connect_new_backend-main-2
mvn clean install
mvn spring-boot:run
```

### 3. Frontend Deployment
```bash
# Start the frontend
cd eara_connect_new_frontend-main
npm start
```

### 4. Verification
1. **Check database** - no NULL user_role or subcommittee_id
2. **Test API endpoints** - all Chair endpoints working
3. **Test frontend** - Chair dashboard loads without errors
4. **Test functionality** - submit reports, update profile

## 🔍 **TROUBLESHOOTING**

### Common Issues:

#### 1. "User is not a Chair" Error
- **Cause**: Database user_role is NULL or incorrect
- **Fix**: Run database migration script
- **Check**: Verify user role in database

#### 2. "Cannot submit report" Error
- **Cause**: Resolution not assigned to Chair's subcommittee
- **Fix**: Assign resolution to Chair's subcommittee
- **Check**: Verify resolution-subcommittee relationship

#### 3. "No assigned resolutions" Message
- **Cause**: Chair has no subcommittee or no resolutions assigned
- **Fix**: Assign Chair to subcommittee and assign resolutions
- **Check**: Verify Chair's subcommittee_id and resolution assignments

#### 4. Frontend Loading Issues
- **Cause**: API endpoints not responding
- **Fix**: Check backend server is running
- **Check**: Verify API base URL in frontend

## 📋 **CHECKLIST**

### Database ✅
- [ ] Run database migration script
- [ ] Verify no NULL user_role values
- [ ] Verify Chair users have subcommittee_id
- [ ] Test Chair role validation

### Backend ✅
- [ ] Deploy ChairValidationService
- [ ] Deploy ChairController
- [ ] Test all API endpoints
- [ ] Verify validation logic

### Frontend ✅
- [ ] Deploy EnhancedChairDashboard
- [ ] Deploy CSS styles
- [ ] Update App.js routing
- [ ] Test all functionality

### Testing ✅
- [ ] Test report submission
- [ ] Test profile updates
- [ ] Test resolution access
- [ ] Test notifications
- [ ] Test error handling

## 🎉 **SUCCESS INDICATORS**

1. **Database**: No NULL values in critical fields
2. **API**: All Chair endpoints return proper responses
3. **Frontend**: Chair dashboard loads without hardcoded data
4. **Functionality**: Chairs can submit reports and update profiles
5. **Validation**: Proper role-based access control working
6. **Notifications**: Chair receives relevant notifications
7. **Error Handling**: Graceful error messages for invalid actions

The Chair role functionality is now fully implemented with proper database fixes, comprehensive validation, and a modern frontend interface without any hardcoded data.
