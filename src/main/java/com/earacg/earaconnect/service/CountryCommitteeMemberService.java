package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.CountryCommitteeMember;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.repository.CountryCommitteeMemberRepo;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import com.earacg.earaconnect.repository.UserRepo;
import com.earacg.earaconnect.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CountryCommitteeMemberService {

    @Autowired
    private CountryCommitteeMemberRepo countryCommitteeMemberRepo;
    
    @Autowired
    private CSubCommitteeMembersRepo cSubCommitteeMembersRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Create
    public CountryCommitteeMember save(CountryCommitteeMember member) {
        log.info("🔍 CountryCommitteeMemberService: Creating/updating Commissioner General member: {}", member.getName());
        validateRoles(member);
        // Save the committee member first
        CountryCommitteeMember savedMember = countryCommitteeMemberRepo.save(member);
        
        // Sync to users table and send credentials if needed
        syncMemberToUser(savedMember);
        
        log.info("✅ CountryCommitteeMemberService: Successfully saved Commissioner General member: {}", savedMember.getName());
        
        return savedMember;
    }

    // Read (single)
    public Optional<CountryCommitteeMember> findById(Long id) {
        return countryCommitteeMemberRepo.findById(id);
    }

    // Read (all)
    public List<CountryCommitteeMember> findAll() {
        return countryCommitteeMemberRepo.findAll();
    }

    // Update
    public CountryCommitteeMember update(CountryCommitteeMember member) {
        log.info("🔍 CountryCommitteeMemberService: Updating Commissioner General member: {}", member.getName());
        validateRoles(member);
        // Update the committee member
        CountryCommitteeMember updatedMember = countryCommitteeMemberRepo.save(member);
        
        // Sync to users table and send credentials if needed
        syncMemberToUser(updatedMember);
        
        log.info("✅ CountryCommitteeMemberService: Successfully updated Commissioner General member: {}", updatedMember.getName());
        
        return updatedMember;
    }

    // Delete
    public void deleteById(Long id) {
        countryCommitteeMemberRepo.deleteById(id);
    }

    /**
     * Get all members from a specific committee by committee ID
     */
    public List<CountryCommitteeMember> getMembersByCommitteeId(Long committeeId) {
        return countryCommitteeMemberRepo.findByCommitteeId(committeeId);
    }
    
    /**
     * Get all chairs from a specific committee
     */
    public List<CountryCommitteeMember> getChairsByCommitteeId(Long committeeId) {
        return countryCommitteeMemberRepo.findChairsByCommitteeId(committeeId);
    }
    
    /**
     * Get all vice chairs from a specific committee
     */
    public List<CountryCommitteeMember> getViceChairsByCommitteeId(Long committeeId) {
        return countryCommitteeMemberRepo.findViceChairsByCommitteeId(committeeId);
    }
    
    /**
     * Get all secretaries from a specific committee
     */
    public List<CountryCommitteeMember> getSecretariesByCommitteeId(Long committeeId) {
        return countryCommitteeMemberRepo.findSecretariesByCommitteeId(committeeId);
    }
    
    /**
     * Get all regular members from a specific committee
     */
    public List<CountryCommitteeMember> getRegularMembersByCommitteeId(Long committeeId) {
        return countryCommitteeMemberRepo.findRegularMembersByCommitteeId(committeeId);
    }
    
    /**
     * Get a specific committee member by ID
     */
    public Optional<CountryCommitteeMember> getMemberById(Long memberId) {
        return countryCommitteeMemberRepo.findById(memberId);
    }
    
    /**
     * Get all committee members
     */
    public List<CountryCommitteeMember> getAllMembers() {
        return countryCommitteeMemberRepo.findAll();
    }

    /**
     * Get HOD members - returns only chairs from "Head of Delegation" committee
     * This ensures we don't get duplicate HOD cards in the UI
     */
    public List<CountryCommitteeMember> getHodMembers() {
        List<CountryCommitteeMember> allHodMembers = countryCommitteeMemberRepo.findByCommitteeNameContaining("head of delegation");
        
        // Filter to only return chairs (actual HODs), not all members of the HOD committee
        List<CountryCommitteeMember> hodChairs = allHodMembers.stream()
            .filter(CountryCommitteeMember::isChair)
            .collect(java.util.stream.Collectors.toList());
        
        log.info("🔍 Found {} HOD members (chairs only) out of {} total HOD committee members", 
                hodChairs.size(), allHodMembers.size());
        
        return hodChairs;
    }

    public List<CountryCommitteeMember> findByCountryId(Long countryId) {
        return countryCommitteeMemberRepo.findByCountryId(countryId);
    }
    
    /**
     * Sync committee member to users table and assign roles. Creates user if not exists, updates roles if exists.
     * This ensures Commissioner Generals can access the system with auto-generated credentials.
     */
    private void syncMemberToUser(CountryCommitteeMember member) {
        if (member.getEmail() == null || member.getEmail().isEmpty()) {
            log.warn("⚠️ CountryCommitteeMemberService: Commissioner General {} has no email, cannot sync to user.", member.getName());
            return;
        }
        
        log.info("🔍 CountryCommitteeMemberService: Syncing Commissioner General {} to user", member.getName());
        log.info("🔍 CountryCommitteeMemberService: Member country: {}", member.getCountry() != null ? member.getCountry().getName() : "NULL");
        log.info("🔍 CountryCommitteeMemberService: Member committee: {}", member.getCommittee() != null ? member.getCommittee().getName() : "NULL");
        
        // Determine the primary role from membership boolean flags (single source of truth)
        User.UserRole primaryRole = member.determineUserRole();
        log.info("🔍 CountryCommitteeMemberService: Determined primary role: {}", primaryRole);
        
        // Check if user exists
        Optional<User> userOpt = userRepo.findByEmail(member.getEmail());
        if (userOpt.isPresent()) {
        User user = userOpt.get();
        
        // CountryCommitteeMember is the authoritative source for CG committee roles.
        // The /members page manages Commissioner General committee positions directly,
        // so role updates from here should always take effect.
        user.setRole(primaryRole);
        
        user.setName(member.getName());
        user.setEmail(member.getEmail());
        user.setCountry(member.getCountry());
        // Committee-level roles must not remain linked to any subcommittee row.
        if (primaryRole == User.UserRole.HOD || primaryRole == User.UserRole.COMMISSIONER_GENERAL) {
            user.setSubcommittee(null);
        }
        user.setActive(true);

        userRepo.save(user);

        log.info("✅ Updated existing user {} with role {} (no password reset/email).",
                user.getEmail(), primaryRole);
        return;
    }
    else {
            // Create new user with auto-generated password
            User newUser = new User();
            newUser.setEmail(member.getEmail());
            newUser.setName(member.getName());
            newUser.setPhone(member.getPhone());
            newUser.setRole(primaryRole);
            newUser.setCountry(member.getCountry());
            newUser.setActive(true);
            
            // Generate random password for new users and encode it
            String randomPassword = generateRandomPassword();
            newUser.setPassword(passwordEncoder.encode(randomPassword));
            
            User savedUser = userRepo.save(newUser);
            log.info("✅ CountryCommitteeMemberService: Created new user {} with role {} and country {}", 
                    newUser.getEmail(), primaryRole, member.getCountry() != null ? member.getCountry().getName() : "NULL");
            
            // Send credentials via email based on role
            try {
                if (primaryRole == User.UserRole.HOD) {
                    emailService.sendHODCredentials(newUser.getEmail(), newUser.getName(), randomPassword);
                    log.info("✅ CountryCommitteeMemberService: Sent HOD credentials email to new user: {}", newUser.getEmail());
                } else if (primaryRole == User.UserRole.COMMISSIONER_GENERAL) {
                    emailService.sendCommissionerGeneralCredentials(newUser.getEmail(), newUser.getName(), randomPassword);
                    log.info("✅ CountryCommitteeMemberService: Sent Commissioner General credentials email to new user: {}", newUser.getEmail());
                } else {
                    // For other committee roles, send generic committee member credentials
                    emailService.sendCommitteeMemberCredentials(newUser.getEmail(), newUser.getName(), randomPassword, primaryRole.toString());
                    log.info("✅ CountryCommitteeMemberService: Sent {} credentials email to new user: {}", primaryRole, newUser.getEmail());
                }
            } catch (Exception e) {
                log.error("❌ CountryCommitteeMemberService: Failed to send credentials email to new user: {}", newUser.getEmail(), e);
            }
        }
    }
    
    /**
     * Generate a secure random password for new users
     */
    private String generateRandomPassword() {
        // Generate a 12-character password with letters, numbers, and special characters
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
        private void validateRoles(CountryCommitteeMember member) {
        // Validate role count (1-2 roles allowed)
        int count = 0;
        if (member.isChair()) count++;
        if (member.isViceChair()) count++;
        if (member.isCommitteeSecretary()) count++;
        if (member.isCommitteeMember()) count++;
        if (member.isDelegationSecretary()) count++;
        if (member.isCommissionerGeneral()) count++;

        if (count < 1) {
            throw new IllegalArgumentException("Select at least 1 role for the committee member.");
        }
        if (count > 2) {
            throw new IllegalArgumentException("A committee member cannot have more than 2 roles.");
        }

        // Validate Chair uniqueness - one per committee
        if (member.isChair() && member.getCommittee() != null && member.getCommittee().getId() != null) {
            List<CountryCommitteeMember> existingChairs = countryCommitteeMemberRepo.findChairsByCommitteeId(member.getCommittee().getId());
            boolean anotherChairExists = existingChairs.stream()
                .anyMatch(m -> member.getId() == null || !m.getId().equals(member.getId()));
            if (anotherChairExists) {
                String existingChairName = existingChairs.stream()
                    .filter(m -> member.getId() == null || !m.getId().equals(member.getId()))
                    .findFirst()
                    .map(CountryCommitteeMember::getName)
                    .orElse("Unknown");
                throw new IllegalArgumentException(
                    String.format("Chair role is already assigned to %s in %s committee.", 
                        existingChairName, member.getCommittee().getName())
                );
            }
        }

        // Validate Committee Secretary uniqueness - one per committee
        if (member.isCommitteeSecretary() && member.getCommittee() != null && member.getCommittee().getId() != null) {
            List<CountryCommitteeMember> existingSecretaries = countryCommitteeMemberRepo.findSecretariesByCommitteeId(member.getCommittee().getId());
            boolean anotherSecretaryExists = existingSecretaries.stream()
                .anyMatch(m -> member.getId() == null || !m.getId().equals(member.getId()));
            if (anotherSecretaryExists) {
                String existingSecretaryName = existingSecretaries.stream()
                    .filter(m -> member.getId() == null || !m.getId().equals(member.getId()))
                    .findFirst()
                    .map(CountryCommitteeMember::getName)
                    .orElse("Unknown");
                throw new IllegalArgumentException(
                    String.format("Committee Secretary role is already assigned to %s in %s committee.", 
                        existingSecretaryName, member.getCommittee().getName())
                );
            }
        }

        // Validate Delegation Secretary uniqueness - one per country across ALL committees and sub-committees
        if (member.isDelegationSecretary() && member.getCountry() != null && member.getCountry().getId() != null) {
            // Check in committee members table
            List<CountryCommitteeMember> existingInCommittees = countryCommitteeMemberRepo.findDelegationSecretariesByCountryId(member.getCountry().getId());
            boolean anotherExistsInCommittees = existingInCommittees.stream()
                .anyMatch(m -> member.getId() == null || !m.getId().equals(member.getId()));
            
            if (anotherExistsInCommittees) {
                String existingName = existingInCommittees.stream()
                    .filter(m -> member.getId() == null || !m.getId().equals(member.getId()))
                    .findFirst()
                    .map(CountryCommitteeMember::getName)
                    .orElse("Unknown");
                throw new IllegalArgumentException(
                    String.format("Delegation Secretary is already assigned to %s for %s.", 
                        existingName, member.getCountry().getName())
                );
            }
            
            // Check in sub-committee members table (cross-table validation)
            List<CSubCommitteeMembers> existingInSubCommittees = cSubCommitteeMembersRepo.findByCountryIdAndIsDelegationSecretaryTrue(member.getCountry().getId());
            if (!existingInSubCommittees.isEmpty()) {
                String existingName = existingInSubCommittees.get(0).getName();
                throw new IllegalArgumentException(
                    String.format("Delegation Secretary is already assigned to %s for %s in a sub-committee.", 
                        existingName, member.getCountry().getName())
                );
            }
            
            log.info("✅ Delegation Secretary validation passed for {} in {}", 
                member.getName(), member.getCountry().getName());
        }
    }

    
    /**
     * Manually resend credentials for an existing Commissioner General
     * This is useful for users who didn't receive their initial credentials
     */
    public boolean resendCredentials(Long memberId) {
        try {
            Optional<CountryCommitteeMember> memberOpt = findById(memberId);
            if (memberOpt.isPresent()) {
                CountryCommitteeMember member = memberOpt.get();
                
                if (member.getEmail() == null || member.getEmail().isEmpty()) {
                    log.warn("⚠️ CountryCommitteeMemberService: Cannot resend credentials - member {} has no email", member.getName());
                    return false;
                }
                
                // Check if user exists in users table
                Optional<User> userOpt = userRepo.findByEmail(member.getEmail());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // Generate new password and encode it
                    String newPassword = generateRandomPassword();
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepo.save(user);
                    
                    // Send new credentials based on role
                    User.UserRole role = member.determineUserRole();
                    if (role == User.UserRole.HOD) {
                        emailService.sendHODCredentials(user.getEmail(), user.getName(), newPassword);
                    } else if (role == User.UserRole.COMMISSIONER_GENERAL) {
                        emailService.sendCommissionerGeneralCredentials(user.getEmail(), user.getName(), newPassword);
                    } else {
                        emailService.sendCommitteeMemberCredentials(user.getEmail(), user.getName(), newPassword, role.toString());
                    }
                    
                    log.info("✅ CountryCommitteeMemberService: Successfully resent credentials to {}: {}", role, member.getName());
                    return true;
                } else {
                    log.warn("⚠️ CountryCommitteeMemberService: User not found for member {}, creating new user", member.getName());
                    // Create new user and send credentials
                    syncMemberToUser(member);
                    return true;
                }
            } else {
                log.warn("⚠️ CountryCommitteeMemberService: Member not found with ID: {}", memberId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ CountryCommitteeMemberService: Error resending credentials for member ID: {}", memberId, e);
            return false;
        }
    }
    
    /**
     * Check if a Commissioner General has a user account with password
     * Returns password status information
     */
    public Map<String, Object> getPasswordStatus(Long memberId) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Optional<CountryCommitteeMember> memberOpt = findById(memberId);
            if (memberOpt.isPresent()) {
                CountryCommitteeMember member = memberOpt.get();
                
                status.put("memberId", memberId);
                status.put("memberName", member.getName());
                status.put("memberEmail", member.getEmail());
                status.put("hasEmail", member.getEmail() != null && !member.getEmail().isEmpty());
                
                if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                    // Check if user exists in users table
                    Optional<User> userOpt = userRepo.findByEmail(member.getEmail());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        status.put("hasUserAccount", true);
                        status.put("userRole", user.getRole());
                        status.put("userActive", user.isActive());
                        status.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
                        status.put("passwordLocation", "users table (secure storage)");
                        status.put("lastLogin", user.getLastLogin());
                    } else {
                        status.put("hasUserAccount", false);
                        status.put("hasPassword", false);
                        status.put("passwordLocation", "No user account exists");
                    }
                } else {
                    status.put("hasUserAccount", false);
                    status.put("hasPassword", false);
                    status.put("passwordLocation", "No email provided");
                }
                
                log.info("🔍 CountryCommitteeMemberService: Password status for member {}: {}", member.getName(), status);
                
            } else {
                status.put("error", "Member not found with ID: " + memberId);
            }
        } catch (Exception e) {
            log.error("❌ CountryCommitteeMemberService: Error checking password status for member ID: {}", memberId, e);
            status.put("error", "Error checking password status: " + e.getMessage());
        }
        
        return status;
    }
}