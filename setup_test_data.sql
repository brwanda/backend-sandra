-- Setup Test Data for Chair Dashboard
-- This script creates the necessary test data for the chair dashboard functionality

-- 1. Create a test subcommittee
INSERT INTO sub_committees (id, name, description, created_at, updated_at)
VALUES (1, 'Technology Subcommittee', 'Handles all technology-related resolutions', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Create a test chair user with ID 1
INSERT INTO users (id, email, password, name, role, subcommittee_id, created_at, updated_at, is_first_login, password_reset_required)
VALUES (1, 'chair@tech.eara.org', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Test Chair', 'CHAIR', 1, NOW(), NOW(), false, false)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    subcommittee_id = EXCLUDED.subcommittee_id,
    updated_at = NOW();

-- 3. Create a test chair user with ID 19 (for the current user)
INSERT INTO users (id, email, password, name, role, subcommittee_id, created_at, updated_at, is_first_login, password_reset_required)
VALUES (19, 'chair19@tech.eara.org', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Chair User 19', 'CHAIR', 1, NOW(), NOW(), false, false)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    subcommittee_id = EXCLUDED.subcommittee_id,
    updated_at = NOW();

-- 4. Create a test resolution
INSERT INTO resolutions (id, title, description, status, created_at, updated_at)
VALUES (1, 'Implement New Technology Infrastructure', 'Upgrade the organization''s technology infrastructure to improve efficiency and security', 'ASSIGNED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 5. Create another test resolution
INSERT INTO resolutions (id, title, description, status, created_at, updated_at)
VALUES (2, 'Digital Transformation Initiative', 'Implement digital transformation across all departments', 'ASSIGNED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 6. Create resolution assignment for the subcommittee
INSERT INTO resolution_assignments (id, resolution_id, subcommittee_id, contribution_percentage, created_at, updated_at)
VALUES (1, 1, 1, 100, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 7. Create another resolution assignment
INSERT INTO resolution_assignments (id, resolution_id, subcommittee_id, contribution_percentage, created_at, updated_at)
VALUES (2, 2, 1, 100, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 8. Create a test meeting (optional)
INSERT INTO meetings (id, title, description, meeting_date, created_at, updated_at)
VALUES (1, 'Technology Committee Meeting', 'Monthly technology committee meeting', NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 9. Link resolutions to meeting
UPDATE resolutions SET meeting_id = 1 WHERE id IN (1, 2);

-- 10. Set the resolution creator (optional)
UPDATE resolutions SET created_by_id = 1 WHERE id IN (1, 2);

-- 11. Create test reports for user 19
INSERT INTO reports (id, resolution_id, subcommittee_id, submitted_by, performance_percentage, progress_details, hindrances, status, submitted_at, created_at, updated_at)
VALUES (1, 1, 1, 19, 85, 'Successfully implemented new server infrastructure and upgraded network security protocols. All systems are now running on the latest technology stack with improved performance and security measures in place.', 'Minor delays due to hardware procurement, but resolved within timeline.', 'SUBMITTED', NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO reports (id, resolution_id, subcommittee_id, submitted_by, performance_percentage, progress_details, hindrances, status, submitted_at, created_at, updated_at)
VALUES (2, 2, 1, 19, 75, 'Digital transformation project is progressing well. Completed 75% of the planned initiatives including cloud migration and digital workflow implementation.', 'Some resistance from legacy system users, but training programs are helping.', 'SUBMITTED', NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 12. Create a test HOD user for review
INSERT INTO users (id, email, password, name, role, created_at, updated_at, is_first_login, password_reset_required)
VALUES (100, 'hod@tech.eara.org', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Test HOD', 'HOD', NOW(), NOW(), false, false)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    updated_at = NOW();

-- Verify the setup
SELECT 'Setup complete!' as status;
SELECT 'Users:' as info, COUNT(*) as count FROM users;
SELECT 'Subcommittees:' as info, COUNT(*) as count FROM sub_committees;
SELECT 'Resolutions:' as info, COUNT(*) as count FROM resolutions;
SELECT 'Assignments:' as info, COUNT(*) as count FROM resolution_assignments;
SELECT 'Reports:' as info, COUNT(*) as count FROM reports;

-- Show specific data for user 19
SELECT 'User 19 details:' as info;
SELECT id, email, name, role, subcommittee_id FROM users WHERE id = 19;

-- Show resolutions for subcommittee 1
SELECT 'Resolutions for subcommittee 1:' as info;
SELECT r.id, r.title, r.status, ra.contribution_percentage 
FROM resolutions r 
JOIN resolution_assignments ra ON r.id = ra.resolution_id 
WHERE ra.subcommittee_id = 1;

-- Show reports for user 19
SELECT 'Reports for user 19:' as info;
SELECT r.id, r.progress_details, r.performance_percentage, r.status, r.submitted_at
FROM reports r 
WHERE r.submitted_by = 19;
