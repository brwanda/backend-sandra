package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import com.earacg.earaconnect.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseTestService {

    private final UserRepo userRepo;
    private final CSubCommitteeMembersRepo cSubCommitteeMembersRepo;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Run all database validation tests
     */
    public DatabaseTestResult runAllTests() {
        log.info("Running comprehensive database validation tests");
        
        DatabaseTestResult result = new DatabaseTestResult();
        
        // Test 1: User creation with role validation
        result.addTest(testUserCreationValidation());
        
        // Test 2: Committee member role assignment
        result.addTest(testCommitteeMemberRoleAssignment());
        
        // Test 3: Login flow tracking
        result.addTest(testLoginFlowTracking());
        
        // Test 4: Role-specific field requirements
        result.addTest(testRoleSpecificRequirements());
        
        // Test 5: Data consistency between tables
        result.addTest(testDataConsistency());
        
        // Test 6: NULL value validation
        result.addTest(testNullValueValidation());
        
        log.info("Completed database validation tests. Passed: {}, Failed: {}", 
                 result.getPassedCount(), result.getFailedCount());
        
        return result;
    }

    private TestCase testUserCreationValidation() {
        TestCase test = new TestCase("User Creation Validation");
        
        try {
            // Test creating a secretary without country (should fail)
            User secretary = new User();
            secretary.setEmail("test-secretary@test.com");
            secretary.setName("Test Secretary");
            secretary.setRole(User.UserRole.SECRETARY);
            // No country set - should fail
            
            try {
                userService.createUser(secretary);
                test.fail("Secretary creation without country should have failed");
            } catch (IllegalArgumentException e) {
                test.pass("Secretary creation properly validated country requirement");
            }
            
            // Test creating a chair without subcommittee (should fail)
            User chair = new User();
            chair.setEmail("test-chair@test.com");
            chair.setName("Test Chair");
            chair.setRole(User.UserRole.CHAIR);
            // No subcommittee set - should fail
            
            try {
                userService.createUser(chair);
                test.fail("Chair creation without subcommittee should have failed");
            } catch (IllegalArgumentException e) {
                test.pass("Chair creation properly validated subcommittee requirement");
            }
            
        } catch (Exception e) {
            test.fail("Unexpected error in user creation validation: " + e.getMessage());
        }
        
        return test;
    }

    private TestCase testCommitteeMemberRoleAssignment() {
        TestCase test = new TestCase("Committee Member Role Assignment");
        
        try {
            // Check that all committee members have user_role assigned
            List<CSubCommitteeMembers> members = cSubCommitteeMembersRepo.findAll();
            long nullRoleCount = members.stream()
                    .filter(member -> member.getUserRole() == null)
                    .count();
            
            if (nullRoleCount == 0) {
                test.pass("All committee members have user_role assigned");
            } else {
                test.fail(String.format("Found %d committee members with NULL user_role", nullRoleCount));
            }
            
            // Check role determination logic
            for (CSubCommitteeMembers member : members.subList(0, Math.min(5, members.size()))) {
                User.UserRole determinedRole = member.determineUserRole();
                if (member.getUserRole() == determinedRole) {
                    test.addDetail(String.format("Member %s: role correctly assigned as %s", 
                                                member.getName(), determinedRole));
                } else {
                    test.fail(String.format("Member %s: role mismatch. Expected %s, got %s", 
                                          member.getName(), determinedRole, member.getUserRole()));
                }
            }
            
        } catch (Exception e) {
            test.fail("Error in committee member role assignment test: " + e.getMessage());
        }
        
        return test;
    }

    private TestCase testLoginFlowTracking() {
        TestCase test = new TestCase("Login Flow Tracking");
        
        try {
            // Check that all users have proper default values
            List<User> users = userRepo.findAll();
            long nullFirstLoginCount = users.stream()
                    .filter(user -> user.getFirstLogin() == null)
                    .count();
            
            if (nullFirstLoginCount == 0) {
                test.pass("All users have is_first_login properly set");
            } else {
                test.fail(String.format("Found %d users with NULL is_first_login", nullFirstLoginCount));
            }
            
            // Test authentication updates login tracking
            if (!users.isEmpty()) {
                User testUser = users.get(0);
                String originalEmail = testUser.getEmail();
                String testPassword = "testpassword123";
                
                // Set a known password for testing (encode it properly)
                testUser.setPassword(passwordEncoder.encode(testPassword));
                userRepo.save(testUser);
                
                boolean authResult = userService.authenticateUser(originalEmail, testPassword);
                if (authResult) {
                    // Reload user to check updates
                    User updatedUser = userRepo.findByEmail(originalEmail).orElse(null);
                    if (updatedUser != null && updatedUser.getLastLogin() != null) {
                        test.pass("Login tracking properly updates last_login");
                    } else {
                        test.fail("Login tracking did not update last_login");
                    }
                } else {
                    test.addDetail("Authentication test skipped - user not active or password mismatch");
                }
            }
            
        } catch (Exception e) {
            test.fail("Error in login flow tracking test: " + e.getMessage());
        }
        
        return test;
    }

    private TestCase testRoleSpecificRequirements() {
        TestCase test = new TestCase("Role-Specific Field Requirements");
        
        try {
            List<User> users = userRepo.findAll();
            int violationCount = 0;
            
            for (User user : users) {
                // Check secretary roles require country
                if ((user.getRole() == User.UserRole.SECRETARY || 
                     user.getRole() == User.UserRole.COMMITTEE_SECRETARY ||
                     user.getRole() == User.UserRole.DELEGATION_SECRETARY) && 
                    user.getCountry() == null) {
                    violationCount++;
                    test.addDetail(String.format("Secretary %s (%s) missing required country", 
                                                user.getName(), user.getRole()));
                }
                
                // Check chair/subcommittee member roles require subcommittee
                if ((user.getRole() == User.UserRole.CHAIR || 
                     user.getRole() == User.UserRole.SUBCOMMITTEE_MEMBER) && 
                    user.getSubcommittee() == null) {
                    violationCount++;
                    test.addDetail(String.format("Chair/Member %s (%s) missing required subcommittee", 
                                                user.getName(), user.getRole()));
                }
            }
            
            if (violationCount == 0) {
                test.pass("All users meet role-specific field requirements");
            } else {
                test.fail(String.format("Found %d role-specific field requirement violations", violationCount));
            }
            
        } catch (Exception e) {
            test.fail("Error in role-specific requirements test: " + e.getMessage());
        }
        
        return test;
    }

    private TestCase testDataConsistency() {
        TestCase test = new TestCase("Data Consistency Between Tables");
        
        try {
            // Check consistency between users and committee members
            List<CSubCommitteeMembers> members = cSubCommitteeMembersRepo.findAll();
            int inconsistencyCount = 0;
            
            for (CSubCommitteeMembers member : members) {
                if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                    User user = userRepo.findByEmail(member.getEmail()).orElse(null);
                    if (user != null) {
                        // Check if roles are compatible
                        User.UserRole memberRole = member.getUserRole();
                        User.UserRole userRole = user.getRole();
                        
                        if (memberRole != null && !memberRole.equals(userRole)) {
                            inconsistencyCount++;
                            test.addDetail(String.format("Role mismatch for %s: committee=%s, user=%s", 
                                                        member.getEmail(), memberRole, userRole));
                        }
                    } else {
                        test.addDetail(String.format("Committee member %s has no corresponding user record", 
                                                    member.getEmail()));
                    }
                }
            }
            
            if (inconsistencyCount == 0) {
                test.pass("Data consistency maintained between tables");
            } else {
                test.fail(String.format("Found %d data consistency issues", inconsistencyCount));
            }
            
        } catch (Exception e) {
            test.fail("Error in data consistency test: " + e.getMessage());
        }
        
        return test;
    }

    private TestCase testNullValueValidation() {
        TestCase test = new TestCase("NULL Value Validation");
        
        try {
            // Check critical NULL values in users table
            List<User> users = userRepo.findAll();
            Map<String, Integer> nullCounts = new HashMap<>();
            
            for (User user : users) {
                if (user.getName() == null || user.getName().trim().isEmpty()) {
                    nullCounts.merge("name", 1, Integer::sum);
                }
                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    nullCounts.merge("email", 1, Integer::sum);
                }
                if (user.getRole() == null) {
                    nullCounts.merge("role", 1, Integer::sum);
                }
            }
            
            // Check critical NULL values in committee members table
            List<CSubCommitteeMembers> members = cSubCommitteeMembersRepo.findAll();
            for (CSubCommitteeMembers member : members) {
                if (member.getUserRole() == null) {
                    nullCounts.merge("committee_user_role", 1, Integer::sum);
                }
            }
            
            if (nullCounts.isEmpty()) {
                test.pass("No critical NULL values found");
            } else {
                StringBuilder details = new StringBuilder("Critical NULL values found: ");
                nullCounts.forEach((field, count) -> 
                    details.append(String.format("%s: %d, ", field, count)));
                test.fail(details.toString());
            }
            
        } catch (Exception e) {
            test.fail("Error in NULL value validation test: " + e.getMessage());
        }
        
        return test;
    }

    /**
     * Test result container
     */
    public static class DatabaseTestResult {
        private List<TestCase> testCases = new ArrayList<>();
        
        public void addTest(TestCase testCase) {
            testCases.add(testCase);
        }
        
        public long getPassedCount() {
            return testCases.stream().filter(TestCase::isPassed).count();
        }
        
        public long getFailedCount() {
            return testCases.stream().filter(tc -> !tc.isPassed()).count();
        }
        
        public List<TestCase> getTestCases() {
            return testCases;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Database Test Results ===\n");
            sb.append(String.format("Total Tests: %d, Passed: %d, Failed: %d\n", 
                                   testCases.size(), getPassedCount(), getFailedCount()));
            sb.append("\n");
            
            for (TestCase testCase : testCases) {
                sb.append(testCase.toString()).append("\n");
            }
            
            return sb.toString();
        }
    }

    /**
     * Individual test case
     */
    public static class TestCase {
        private String name;
        private boolean passed = true;
        private String failureReason;
        private List<String> details = new ArrayList<>();
        
        public TestCase(String name) {
            this.name = name;
        }
        
        public void pass(String message) {
            this.passed = true;
            this.details.add("✓ " + message);
        }
        
        public void fail(String reason) {
            this.passed = false;
            this.failureReason = reason;
        }
        
        public void addDetail(String detail) {
            this.details.add("  " + detail);
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[%s] %s\n", passed ? "PASS" : "FAIL", name));
            if (!passed && failureReason != null) {
                sb.append("  ✗ ").append(failureReason).append("\n");
            }
            for (String detail : details) {
                sb.append("  ").append(detail).append("\n");
            }
            return sb.toString();
        }
    }
}
