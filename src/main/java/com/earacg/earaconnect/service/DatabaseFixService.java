package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import com.earacg.earaconnect.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DatabaseFixService {

    private final UserRepo userRepo;
    private final CSubCommitteeMembersRepo cSubCommitteeMembersRepo;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Fix all NULL user_role entries in csub_committee_members table
     */
    public void fixNullUserRoles() {
        log.info("Starting to fix NULL user_role entries in csub_committee_members");
        
        List<CSubCommitteeMembers> membersWithNullRoles = cSubCommitteeMembersRepo.findAll()
                .stream()
                .filter(member -> member.getUserRole() == null)
                .toList();
        
        log.info("Found {} committee members with NULL user_role", membersWithNullRoles.size());
        
        for (CSubCommitteeMembers member : membersWithNullRoles) {
            User.UserRole determinedRole = member.determineUserRole();
            member.setUserRole(determinedRole);
            cSubCommitteeMembersRepo.save(member);
            log.info("Fixed user_role for member {}: set to {}", member.getName(), determinedRole);
        }
        
        log.info("Completed fixing NULL user_role entries");
    }

    /**
     * Update is_first_login for users who have never logged in
     */
    public void fixFirstLoginFlags() {
        log.info("Starting to fix is_first_login flags");
        
        List<User> users = userRepo.findAll();
        int updatedCount = 0;
        
        for (User user : users) {
            if (user.getLastLogin() != null && (user.getFirstLogin() == null || user.getFirstLogin())) {
                user.setFirstLogin(false);
                userRepo.save(user);
                updatedCount++;
            }
        }
        
        log.info("Updated is_first_login flag for {} users", updatedCount);
    }

    /**
     * Generate a report of missing required fields by role
     */
    public DatabaseIntegrityReport generateIntegrityReport() {
        log.info("Generating database integrity report");
        
        DatabaseIntegrityReport report = new DatabaseIntegrityReport();
        
        // Check users table issues
        List<User> allUsers = userRepo.findAll();
        for (User user : allUsers) {
            if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
                report.addMissingPhoneUser(user);
            }
            
            if (user.getCountry() == null && requiresCountry(user.getRole())) {
                report.addMissingCountryUser(user);
            }
            
            if (user.getSubcommittee() == null && requiresSubcommittee(user.getRole())) {
                report.addMissingSubcommitteeUser(user);
            }
            
            if (user.getLastLogin() == null) {
                report.addNeverLoggedInUser(user);
            }
        }
        
        // Check csub_committee_members table issues
        List<CSubCommitteeMembers> allMembers = cSubCommitteeMembersRepo.findAll();
        for (CSubCommitteeMembers member : allMembers) {
            if (member.getUserRole() == null) {
                report.addNullUserRoleMember(member);
            }
        }
        
        return report;
    }

    /**
     * Attempt to auto-fix missing data where possible
     */
    public void autoFixMissingData() {
        log.info("Starting auto-fix for missing data");
        
        // Fix NULL user_role in committee members
        fixNullUserRoles();
        
        // Fix first login flags
        fixFirstLoginFlags();
        
        // Set default values for new fields
        jdbcTemplate.execute("UPDATE users SET is_first_login = TRUE WHERE is_first_login IS NULL");
        jdbcTemplate.execute("UPDATE users SET password_reset_required = FALSE WHERE password_reset_required IS NULL");
        
        log.info("Completed auto-fix for missing data");
    }

    private boolean requiresCountry(User.UserRole role) {
        return role == User.UserRole.SECRETARY || 
               role == User.UserRole.COMMITTEE_SECRETARY || 
               role == User.UserRole.DELEGATION_SECRETARY;
    }

    private boolean requiresSubcommittee(User.UserRole role) {
        return role == User.UserRole.CHAIR || 
               role == User.UserRole.SUBCOMMITTEE_MEMBER;
    }

    /**
     * Report class for database integrity issues
     */
    public static class DatabaseIntegrityReport {
        private int missingPhoneCount = 0;
        private int missingCountryCount = 0;
        private int missingSubcommitteeCount = 0;
        private int neverLoggedInCount = 0;
        private int nullUserRoleCount = 0;
        
        private StringBuilder details = new StringBuilder();
        
        public void addMissingPhoneUser(User user) {
            missingPhoneCount++;
            details.append(String.format("Missing phone: User %s (%s)\n", user.getName(), user.getEmail()));
        }
        
        public void addMissingCountryUser(User user) {
            missingCountryCount++;
            details.append(String.format("Missing country: User %s (%s) with role %s\n", 
                user.getName(), user.getEmail(), user.getRole()));
        }
        
        public void addMissingSubcommitteeUser(User user) {
            missingSubcommitteeCount++;
            details.append(String.format("Missing subcommittee: User %s (%s) with role %s\n", 
                user.getName(), user.getEmail(), user.getRole()));
        }
        
        public void addNeverLoggedInUser(User user) {
            neverLoggedInCount++;
            details.append(String.format("Never logged in: User %s (%s)\n", user.getName(), user.getEmail()));
        }
        
        public void addNullUserRoleMember(CSubCommitteeMembers member) {
            nullUserRoleCount++;
            details.append(String.format("NULL user_role: Committee member %s (%s)\n", 
                member.getName(), member.getEmail()));
        }
        
        @Override
        public String toString() {
            StringBuilder summary = new StringBuilder();
            summary.append("=== Database Integrity Report ===\n");
            summary.append(String.format("Missing phone numbers: %d\n", missingPhoneCount));
            summary.append(String.format("Missing required countries: %d\n", missingCountryCount));
            summary.append(String.format("Missing required subcommittees: %d\n", missingSubcommitteeCount));
            summary.append(String.format("Users never logged in: %d\n", neverLoggedInCount));
            summary.append(String.format("Committee members with NULL user_role: %d\n", nullUserRoleCount));
            summary.append("\n=== Details ===\n");
            summary.append(details.toString());
            return summary.toString();
        }
        
        public boolean hasIssues() {
            return missingPhoneCount > 0 || missingCountryCount > 0 || 
                   missingSubcommitteeCount > 0 || nullUserRoleCount > 0;
        }
    }
}
