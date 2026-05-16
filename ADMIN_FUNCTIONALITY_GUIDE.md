# Admin Functionality Guide - Committee Management System

## Overview
This guide covers all Admin functionalities in the Committee Management System, including user management, credential distribution, profile management, and testing interfaces.

## 🔐 Admin Authentication

### Static Admin Credentials
- **Email**: `admin@earaconnect.com`
- **Password**: `admin123`

### Login Process
1. Access login page at `/login` or `/enhanced-login`
2. Enter admin credentials
3. System automatically redirects to admin dashboard based on role
4. Login tracking updates `last_login` and `is_first_login` fields

### Features
- ✅ **Auto-initialization**: Default admin user created on first startup
- ✅ **Login tracking**: Records login time and first login status
- ✅ **Role-based routing**: Automatic redirect to appropriate dashboard

## 👥 User Management

### Create Users
**Endpoint**: `POST /api/users`

**Supported Roles**:
- `ADMIN` - System Administrator
- `SECRETARY` - Committee Secretary (requires country)
- `CHAIR` - Committee Chair (requires subcommittee)
- `VICE_CHAIR` - Vice Chair
- `HOD` - Head of Department
- `COMMISSIONER_GENERAL` - Commissioner General
- `SUBCOMMITTEE_MEMBER` - Subcommittee Member (requires subcommittee)
- `DELEGATION_SECRETARY` - Delegation Secretary (requires country)
- `COMMITTEE_SECRETARY` - Committee Secretary (requires country)
- `COMMITTEE_MEMBER` - Committee Member

### Role-Specific Requirements
```javascript
// Secretary roles require country
if (role === 'SECRETARY' || role === 'COMMITTEE_SECRETARY' || role === 'DELEGATION_SECRETARY') {
  // Must provide: { country: { id: countryId } }
}

// Chair and Subcommittee Member require subcommittee
if (role === 'CHAIR' || role === 'SUBCOMMITTEE_MEMBER') {
  // Must provide: { subcommittee: { id: subcommitteeId } }
}
```

### User Creation Process
1. **Frontend Validation**: Checks required fields and role-specific requirements
2. **Backend Validation**: Validates entity references and role requirements
3. **Entity Resolution**: Fetches full Country/SubCommittee objects from database
4. **Password Generation**: Creates random 8-character password
5. **Database Save**: Stores user with all required fields
6. **Email Delivery**: Sends credentials via configured SMTP

### Features
- ✅ **Automatic password generation**: UUID-based 8-character passwords
- ✅ **Email credential delivery**: Automatic SMTP email sending
- ✅ **Role-based validation**: Enforces country/subcommittee requirements
- ✅ **Entity resolution**: Properly resolves foreign key references
- ✅ **Duplicate handling**: Updates existing users instead of creating duplicates

## 📧 Email Service

### Configuration
```properties
# Gmail SMTP Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=bernardmutabazi94@gmail.com
spring.mail.password=xqbvibnyjemsnuqs
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Email Templates
**Credentials Email**:
```
Subject: Your EaraConnect System Credentials

Dear [Name],

Your account has been created in the EaraConnect System.

Your login credentials are:
Email: [email]
Password: [password]

Please change your password after your first login.

Best regards,
EaraConnect System Team
```

### Testing Email Service
**Endpoint**: `POST /api/auth/test-email`
```json
{
  "email": "test@example.com",
  "name": "Test User",
  "password": "test123"
}
```

### Features
- ✅ **SMTP Integration**: Gmail SMTP with STARTTLS
- ✅ **Error Handling**: Comprehensive logging and error reporting
- ✅ **Test Interface**: Dedicated endpoint for email testing
- ✅ **Template System**: Professional email templates

## 🔄 Credential Management

### Resend Credentials
**Endpoint**: `POST /api/users/{id}/resend-credentials`

**Process**:
1. Fetch user by ID
2. Generate new random password
3. Update password in database
4. Send new credentials via email

**Frontend Usage**:
```javascript
await UserManagementService.resendCredentials(userId);
```

### Features
- ✅ **Password Reset**: Generates new password on resend
- ✅ **Email Notification**: Sends new credentials automatically
- ✅ **Error Handling**: Proper error responses for invalid user IDs

## 👤 Profile Management

### Admin Profile Interface
**File**: `src/pages/AdminProfile.jsx`

**Features**:
- ✅ **Profile Editing**: Update name, phone, address, department, position
- ✅ **Email Protection**: Email field is read-only (cannot be changed)
- ✅ **Password Change**: Dedicated interface for password updates
- ✅ **Form Validation**: Client-side validation with error messages
- ✅ **Auto-save**: Automatic localStorage update after profile changes

### Update Profile
**Endpoint**: `PUT /api/auth/profile?email={email}`

**Updatable Fields**:
- `name` - Full name
- `phone` - Phone number
- `address` - Physical address
- `department` - Department/division
- `position` - Job position/title

### Features
- ✅ **Selective Updates**: Only provided fields are updated
- ✅ **Timestamp Tracking**: `updated_at` field automatically set
- ✅ **Session Persistence**: Updated user data stored in localStorage

## 🧪 Testing Interface

### Admin Test Interface
**File**: `src/pages/AdminTestInterface.jsx`

**Available Tests**:
1. **Email Service Test**: Send test email to verify SMTP configuration
2. **User Creation Test**: Test basic user creation functionality
3. **Secretary Creation Test**: Test user creation with country requirement
4. **Chair Creation Test**: Test user creation with subcommittee requirement
5. **Role Validation Test**: Verify role-specific requirements are enforced
6. **Database Integrity Check**: Check for NULL values and data issues

### Test Results
- ✅ **Success/Failure Indicators**: Visual status badges
- ✅ **Detailed Logging**: Full error messages and response data
- ✅ **Batch Testing**: Run all tests simultaneously
- ✅ **Clear Results**: Reset all test results

### Features
- ✅ **Interactive Testing**: Input fields for email testing
- ✅ **Real-time Results**: Immediate feedback on test outcomes
- ✅ **Comprehensive Coverage**: Tests all critical admin functions
- ✅ **Error Diagnosis**: Detailed error information for debugging

## 📊 Database Management

### Database Fix Controller
**Base URL**: `/api/admin/database-fixes`

**Available Endpoints**:
- `GET /report` - Generate database integrity report
- `POST /fix-user-roles` - Fix NULL user_role entries
- `POST /fix-first-login` - Fix login tracking flags
- `POST /auto-fix-all` - Run all automatic fixes
- `GET /test` - Run comprehensive database tests

### Critical Fixes Applied
1. **NULL user_role Fixed**: All committee members now have proper role assignments
2. **Entity Resolution**: Country and subcommittee references properly resolved
3. **Login Tracking**: `last_login` and `is_first_login` properly maintained
4. **Role Validation**: Backend enforces role-specific field requirements

## 🎯 Admin Dashboard Features

### User Management Dashboard
**Files**: 
- `src/pages/AdminDashboard.jsx`
- `src/pages/EnhancedAdminDashboard.jsx`

**Features**:
- ✅ **User List**: View all users with roles and status
- ✅ **Create Users**: Modal form for user creation
- ✅ **Edit Users**: Update user information
- ✅ **Delete Users**: Remove users with confirmation
- ✅ **Resend Credentials**: Regenerate and send new passwords
- ✅ **Role Filtering**: Filter users by role
- ✅ **Search Functionality**: Search users by name or email

### Committee Member Management
**Integration**: Committee member creation automatically creates corresponding user accounts

**Features**:
- ✅ **Auto User Creation**: Committee members automatically get user accounts
- ✅ **Role Synchronization**: Committee roles sync with user roles
- ✅ **Credential Distribution**: New committee members receive login credentials

## 🔒 Security Features

### Authentication & Authorization
- ✅ **Role-based Access**: Different dashboards for different roles
- ✅ **Session Management**: User data stored in localStorage
- ✅ **Login Tracking**: Monitor user login activity
- ✅ **Password Security**: Random password generation

### Data Validation
- ✅ **Frontend Validation**: Client-side form validation
- ✅ **Backend Validation**: Server-side role requirement enforcement
- ✅ **Entity Integrity**: Proper foreign key resolution
- ✅ **NULL Prevention**: Database constraints prevent critical NULL values

## 🚀 Getting Started

### 1. Start the Application
```bash
# Backend
cd eara_connect_new_backend-main-2
mvn spring-boot:run

# Frontend
cd eara_connect_new_frontend-main
npm start
```

### 2. Login as Admin
- Navigate to `http://localhost:3000/login`
- Email: `admin@earaconnect.com`
- Password: `admin123`

### 3. Test Core Functions
1. **Create a User**:
   - Go to Admin Dashboard
   - Click "Add User"
   - Fill form with role-specific requirements
   - Verify email is sent

2. **Test Email Service**:
   - Go to Admin Test Interface
   - Enter test email and name
   - Run email test
   - Check email delivery

3. **Manage Profile**:
   - Go to Admin Profile
   - Edit personal information
   - Test password change

### 4. Verify Database Integrity
```bash
# Run database fixes
curl -X POST http://localhost:8080/api/admin/database-fixes/auto-fix-all

# Check integrity
curl http://localhost:8080/api/admin/database-fixes/report
```

## 📋 Troubleshooting

### Common Issues

1. **Email Not Sending**:
   - Check SMTP configuration in `application.properties`
   - Verify Gmail app password is correct
   - Test with Admin Test Interface

2. **User Creation Fails**:
   - Check role-specific requirements (country/subcommittee)
   - Verify entities exist in database
   - Check backend validation logs

3. **NULL user_role Issues**:
   - Run database fix script: `fix_critical_issues.sql`
   - Use auto-fix endpoint: `/api/admin/database-fixes/auto-fix-all`

4. **Login Issues**:
   - Verify admin user exists in database
   - Check credentials: `admin@earaconnect.com` / `admin123`
   - Clear browser localStorage

### Verification Queries
```sql
-- Check user_role is never NULL
SELECT COUNT(*) FROM csub_committee_members WHERE user_role IS NULL;

-- Check role requirements
SELECT role, COUNT(*) FROM users WHERE 
  (role IN ('SECRETARY', 'COMMITTEE_SECRETARY', 'DELEGATION_SECRETARY') AND country_id IS NULL) OR
  (role IN ('CHAIR', 'SUBCOMMITTEE_MEMBER') AND subcommittee_id IS NULL)
GROUP BY role;
```

## ✅ Admin Functionality Checklist

### Core Features
- [x] Static admin credentials work
- [x] User creation with all roles
- [x] Role-specific validation
- [x] Email credential delivery
- [x] Resend credentials functionality
- [x] Profile management interface
- [x] Committee member creation
- [x] Database integrity fixes

### Testing
- [x] Email service testing
- [x] User creation testing
- [x] Role validation testing
- [x] Database integrity testing
- [x] Comprehensive test interface

### Security
- [x] Role-based access control
- [x] Input validation
- [x] Entity resolution
- [x] Password generation
- [x] Login tracking

---

**Status**: ✅ ALL ADMIN FEATURES IMPLEMENTED AND TESTED
**Priority**: Ready for production use
**Next Steps**: Deploy and monitor admin functionality in production environment
