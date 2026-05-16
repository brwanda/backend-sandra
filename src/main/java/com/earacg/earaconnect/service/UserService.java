package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.Country;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.CountryCommitteeMember;
import com.earacg.earaconnect.repository.UserRepo;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import com.earacg.earaconnect.repository.CountryCommitteeMemberRepo;
import com.earacg.earaconnect.repository.MeetingInvitationRepo;
import com.earacg.earaconnect.repository.NotificationRepo;
import com.earacg.earaconnect.repository.AttendanceRepo;
import com.earacg.earaconnect.repository.MeetingRepo;
import com.earacg.earaconnect.repository.ResolutionRepo;
import com.earacg.earaconnect.service.CountryService;
import com.earacg.earaconnect.service.SubCommitteeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private SubCommitteeService subCommitteeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CSubCommitteeMembersRepo cSubCommitteeMembersRepo;

    @Autowired
    private CountryCommitteeMemberRepo countryCommitteeMemberRepo;

    @Autowired
    private MeetingInvitationRepo meetingInvitationRepo;

    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private MeetingRepo meetingRepo;

    @Autowired
    private ResolutionRepo resolutionRepo;

    /**
     * Migrate all plaintext passwords to BCrypt.
     * Detects plaintext passwords by checking if they DON'T start with "$2a$" or "$2b$".
     * Returns the count of passwords migrated.
     */
    public int migratePasswordsToBCrypt() {
        List<User> allUsers = userRepo.findAll();
        int migrated = 0;
        for (User user : allUsers) {
            String pwd = user.getPassword();
            if (pwd != null && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(pwd));
                userRepo.save(user);
                migrated++;
            }
        }
        return migrated;
    }

    public List<User> getAllUsers() {
        List<User> users = userRepo.findAll();
        
        // Populate committee information for users from country_committee_member table
        for (User user : users) {
            if (user.getEmail() != null) {
                Optional<CountryCommitteeMember> committeeMember = 
                    countryCommitteeMemberRepo.findByEmail(user.getEmail());
                
                if (committeeMember.isPresent() && committeeMember.get().getCommittee() != null) {
                    user.setCommittee(committeeMember.get().getCommittee());
                }
            }
        }
        
        return users;
    }

    public Optional<User> getUserById(Long id) {
        return userRepo.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public List<User> getUsersByRole(User.UserRole role) {
        return userRepo.findByRole(role);
    }

    public List<User> getUsersByCountry(Long countryId) {
        return userRepo.findByCountryId(countryId);
    }

    public User createUser(User user) {
        // Resolve country and subcommittee references if they have only ID
        resolveEntityReferences(user);

        // Validate role-specific requirements
        validateUserRoleRequirements(user);

        // Check if user already exists — reject duplicate
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("A user with email '" + user.getEmail() + "' already exists");
        }

        // Generate random password for new users and hash it
        String randomPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(randomPassword));

        User savedUser = userRepo.save(user);

        // Send credentials via email
        emailService.sendCredentials(user.getEmail(), user.getName(), randomPassword);

        return savedUser;
    }

    public User updateUser(Long id, User userDetails) {
        // Resolve country and subcommittee references if they have only ID
        resolveEntityReferences(userDetails);

        Optional<User> userOpt = userRepo.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            User.UserRole previousRole = user.getRole();

            user.setName(userDetails.getName());
            user.setPhone(userDetails.getPhone());
            user.setEmail(userDetails.getEmail());

            // Update additional fields if provided
            if (userDetails.getAddress() != null) {
                user.setAddress(userDetails.getAddress());
            }
            if (userDetails.getDepartment() != null) {
                user.setDepartment(userDetails.getDepartment());
            }
            if (userDetails.getPosition() != null) {
                user.setPosition(userDetails.getPosition());
            }
            if (userDetails.getCountry() != null) {
                user.setCountry(userDetails.getCountry());
            }
            if (userDetails.getSubcommittee() != null) {
                user.setSubcommittee(userDetails.getSubcommittee());
            }

            // Sync role if provided
            if (userDetails.getRole() != null) {
                user.setRole(userDetails.getRole());
            }

            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                // Only encode if the password is NOT already BCrypt-encoded (prevents double-encoding)
                String pwd = userDetails.getPassword();
                if (pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$")) {
                    // Already encoded — keep as-is
                    user.setPassword(pwd);
                } else {
                    user.setPassword(passwordEncoder.encode(pwd));
                }
            }

            User savedUser = userRepo.save(user);

            // If role changed, propagate to membership records
            if (userDetails.getRole() != null && userDetails.getRole() != previousRole) {
                syncUserRoleToMemberships(savedUser);
            }

            return savedUser;
        }
        return null;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepo.existsById(id)) {
            // Delete all related records to avoid foreign key constraint violations
            // Order matters: delete child records first, then nullify references, then delete parent
            
            // 1. Delete meeting invitations (user is a participant)
            meetingInvitationRepo.deleteByUserId(id);
            
            // 2. Delete notifications (user is the recipient)
            notificationRepo.deleteByUserId(id);
            
            // 3. Delete attendance records (user attended meetings)
            attendanceRepo.deleteByUserId(id);
            
            // 4. Nullify createdBy references in meetings (preserve meeting history)
            meetingRepo.nullifyCreatedByForUser(id);
            
            // 5. Nullify createdBy references in resolutions (preserve resolution history)
            resolutionRepo.nullifyCreatedByForUser(id);
            
            // 6. Finally, delete the user
            userRepo.deleteById(id);
            
            return true;
        }
        return false;
    }

    public boolean authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean passwordMatch = passwordEncoder.matches(password, user.getPassword());
            boolean isActive = user.isActive();

            if (passwordMatch && isActive) {
                // Update last login and first login flag
                user.setLastLogin(java.time.LocalDateTime.now());
                if (user.getFirstLogin() == null || user.getFirstLogin()) {
                    user.setFirstLogin(false);
                }
                userRepo.save(user);
                return true;
            }
        }
        return false;
    }

    /**
     * Apply login side effects after a successful authentication
     * without performing password verification here.
     */
    public void recordSuccessfulLogin(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(java.time.LocalDateTime.now());
            if (user.getFirstLogin() == null || user.getFirstLogin()) {
                user.setFirstLogin(false);
            }
            userRepo.save(user);
        });
    }

    public User getAdminUser() {
        List<User> admins = userRepo.findByRole(User.UserRole.ADMIN);
        if (!admins.isEmpty()) {
            return admins.get(0);
        }
        return null;
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Resend credentials to an existing user with a new password
     */
    public User resendCredentials(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Generate new password and hash it
            String newPassword = generateRandomPassword();
            user.setPassword(passwordEncoder.encode(newPassword));

            // Save user with new password
            User savedUser = userRepo.save(user);

            // Send credentials via email
            emailService.sendCredentials(user.getEmail(), user.getName(), newPassword);

            return savedUser;
        }
        throw new IllegalArgumentException("User not found with ID: " + userId);
    }

    /**
     * Validate role-specific requirements for users
     */
    private void validateUserRoleRequirements(User user) {
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role is required");
        }

        // Secretary role requires country_id
        if (user.getRole() == User.UserRole.SECRETARY && user.getCountry() == null) {
            throw new IllegalArgumentException("Country is required for SECRETARY role");
        }

        // Chair and Subcommittee Member roles require subcommittee_id
        if ((user.getRole() == User.UserRole.CHAIR || user.getRole() == User.UserRole.SUBCOMMITTEE_MEMBER)
                && user.getSubcommittee() == null) {
            throw new IllegalArgumentException("Subcommittee is required for CHAIR and SUBCOMMITTEE_MEMBER roles");
        }

        // Committee Secretary requires country_id (location-restricted)
        if (user.getRole() == User.UserRole.COMMITTEE_SECRETARY && user.getCountry() == null) {
            throw new IllegalArgumentException("Country is required for COMMITTEE_SECRETARY role");
        }

        // Delegation Secretary requires country_id
        if (user.getRole() == User.UserRole.DELEGATION_SECRETARY && user.getCountry() == null) {
            throw new IllegalArgumentException("Country is required for DELEGATION_SECRETARY role");
        }
    }

    /**
     * Resolve entity references by fetching full objects from database when only ID
     * is provided
     */
    private void resolveEntityReferences(User user) {
        // Resolve country reference if only ID is provided
        if (user.getCountry() != null && user.getCountry().getId() != null) {
            try {
                Country fullCountry = countryService.getCountryById(user.getCountry().getId());
                user.setCountry(fullCountry);
            } catch (Exception e) {
                throw new IllegalArgumentException("Country not found with ID: " + user.getCountry().getId());
            }
        }

        // Resolve subcommittee reference if only ID is provided
        if (user.getSubcommittee() != null && user.getSubcommittee().getId() != null) {
            SubCommittee fullSubcommittee = subCommitteeService.getSubCommitteeById(user.getSubcommittee().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subcommittee not found with ID: " + user.getSubcommittee().getId()));
            user.setSubcommittee(fullSubcommittee);
        }
    }

    /**
     * Change user password
     * 
     * @param userId          User ID
     * @param currentPassword Current password for verification
     * @param newPassword     New password to set
     * @return true if password changed successfully, false if current password is
     *         incorrect
     */
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify current password
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                // Set new password (hashed)
                user.setPassword(passwordEncoder.encode(newPassword));

                // Update password changed timestamp if the field exists
                try {
                    // Try to set passwordChangedAt if the field exists in the User model
                    user.getClass().getMethod("setPasswordChangedAt", java.time.LocalDateTime.class)
                            .invoke(user, java.time.LocalDateTime.now());
                } catch (Exception e) {
                    // Field doesn't exist, ignore
                    System.out.println("Password changed timestamp field not available");
                }

                userRepo.save(user);
                return true;
            }
        }
        return false;
    }

    public void generatePasswordResetToken(String email) {
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(java.time.LocalDateTime.now().plusHours(24));
            userRepo.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        }
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepo.findByResetToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getResetTokenExpiry() != null &&
                    user.getResetTokenExpiry().isAfter(java.time.LocalDateTime.now())) {

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepo.save(user);
                return true;
            }
        }
        return false;
    }

    /**
     * Sync User.role from the boolean flags on the user's membership records.
     * Looks up CSubCommitteeMembers and CountryCommitteeMember by email,
     * derives the role via determineUserRole(), and updates User.role.
     * Call this wherever membership boolean flags change.
     *
     * @param userId the ID of the User to sync
     * @return the updated User, or null if user not found
     */
    public User syncUserRoleFromBooleans(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();

        // COMMISSIONER_GENERAL, ADMIN, and SECRETARY are user-level roles
        // that should NOT be overridden by membership booleans.
        User.UserRole currentRole = user.getRole();
        if (currentRole == User.UserRole.COMMISSIONER_GENERAL
                || currentRole == User.UserRole.ADMIN || currentRole == User.UserRole.SECRETARY) {
            return user;
        }

        String email = user.getEmail();

        // Check CSubCommitteeMembers first (subcommittee membership)
        List<CSubCommitteeMembers> subMembers = cSubCommitteeMembersRepo.findByEmailIgnoreCase(email);
        if (!subMembers.isEmpty()) {
            // Use the first matching record (highest-priority role wins)
            User.UserRole derivedRole = subMembers.get(0).determineUserRole();
            for (CSubCommitteeMembers m : subMembers) {
                User.UserRole r = m.determineUserRole();
                if (rolePriority(r) < rolePriority(derivedRole)) {
                    derivedRole = r;
                }
            }
            user.setRole(derivedRole);
            return userRepo.save(user);
        }

        // Fallback to CountryCommitteeMember (committee-level membership)
        List<CountryCommitteeMember> committeeMembers = countryCommitteeMemberRepo.findByEmailIgnoreCase(email);
        if (!committeeMembers.isEmpty()) {
            User.UserRole derivedRole = committeeMembers.get(0).determineUserRole();
            for (CountryCommitteeMember m : committeeMembers) {
                User.UserRole r = m.determineUserRole();
                if (rolePriority(r) < rolePriority(derivedRole)) {
                    derivedRole = r;
                }
            }
            user.setRole(derivedRole);
            return userRepo.save(user);
        }

        // No membership records found — leave User.role as-is
        return user;
    }

    /**
     * Propagate a User.role change to all matching CSubCommitteeMembers and
     * CountryCommitteeMember records (reverse sync).
     * Called when Admin updates User.role via the dashboard.
     *
     * @param user the User whose role was just changed
     */
    public void syncUserRoleToMemberships(User user) {
        String email = user.getEmail();
        User.UserRole newRole = user.getRole();

        // COMMISSIONER_GENERAL, ADMIN, and SECRETARY remain user-level roles.
        if (newRole == User.UserRole.COMMISSIONER_GENERAL
                || newRole == User.UserRole.ADMIN || newRole == User.UserRole.SECRETARY) {
            return;
        }

        // Update CSubCommitteeMembers records
        List<CSubCommitteeMembers> subMembers = cSubCommitteeMembersRepo.findByEmailIgnoreCase(email);
        for (CSubCommitteeMembers member : subMembers) {
            member.applyRoleBooleans(newRole);
            cSubCommitteeMembersRepo.save(member);
        }

        // Update CountryCommitteeMember records
        List<CountryCommitteeMember> committeeMembers = countryCommitteeMemberRepo.findByEmailIgnoreCase(email);
        for (CountryCommitteeMember member : committeeMembers) {
            member.applyRoleBooleans(newRole);
            countryCommitteeMemberRepo.save(member);
        }
    }

    /**
     * Role priority for choosing the highest-privilege role when a user has
     * multiple membership records. Lower number = higher priority.
     */
    private int rolePriority(User.UserRole role) {
        switch (role) {
            case ADMIN: return 0;
            case COMMISSIONER_GENERAL: return 1;
            case HOD: return 2;
            case CHAIR: return 3;
            case VICE_CHAIR: return 4;
            case SECRETARY: return 5;
            case COMMITTEE_SECRETARY: return 6;
            case DELEGATION_SECRETARY: return 7;
            case COMMITTEE_MEMBER: return 8;
            case SUBCOMMITTEE_MEMBER: return 9;
            default: return 10;
        }
    }

}