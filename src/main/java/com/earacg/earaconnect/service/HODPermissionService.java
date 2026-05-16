package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.repository.UserRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service to handle HOD (Head of Delegation) permissions.
 */
@Service
public class HODPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(HODPermissionService.class);
    
    private static final String HEAD_OF_DELEGATION_SUBCOMMITTEE_NAME = "Head Of Delegation";

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    /**
     * Check if a user has HOD dashboard privileges.
     */
    public boolean hasHODPrivileges(Long userId) {
        try {
            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User with ID {} not found", userId);
                return false;
            }

            User user = userOpt.get();
            return hasHODPrivileges(user);
        } catch (Exception e) {
            logger.error("Error checking HOD privileges for user ID {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user has HOD dashboard privileges (overloaded with User object).
     */
    public boolean hasHODPrivileges(User user) {
        if (user == null) {
            return false;
        }

        try {
            // Backward-compatible direct HOD role support.
            if (User.UserRole.HOD.equals(user.getRole())) {
                return true;
            }

            // Chair/Vice Chair of Head Of Delegation subcommittee have HOD access.
            if (User.UserRole.CHAIR.equals(user.getRole()) || User.UserRole.VICE_CHAIR.equals(user.getRole())) {
                return isHeadOfDelegationSubcommittee(user);
            }

            return false;
        } catch (Exception e) {
            logger.error("Error checking HOD privileges for user {}: {}", user.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Only Chair-designated HOD can approve/reject reports.
     */
    public boolean canApproveOrRejectReports(User user) {
        return isHODChair(user);
    }

    /**
     * Any HOD user can add comments where comments are allowed.
     */
    public boolean canCommentOnReports(User user) {
        return hasHODPrivileges(user);
    }

    /**
     * Determine if user is designated as Chair of Head Of Delegation.
     */
    public boolean isHODChair(User user) {
        if (user == null) {
            return false;
        }

        // Direct HOD role is treated as chair designation.
        if (User.UserRole.HOD.equals(user.getRole())) {
            return true;
        }

        return User.UserRole.CHAIR.equals(user.getRole()) && isHeadOfDelegationSubcommittee(user);
    }

    /**
     * Check if user is Chair/Vice Chair of Head Of Delegation subcommittee
     */
    private boolean isHeadOfDelegationSubcommittee(User user) {
        try {
            if (user.getSubcommittee() == null) {
                logger.debug("User {} has no subcommittee assigned", user.getId());
                return false;
            }

            SubCommittee subcommittee = user.getSubcommittee();
            boolean isHeadOfDelegation = HEAD_OF_DELEGATION_SUBCOMMITTEE_NAME.equals(subcommittee.getName());
            
            if (isHeadOfDelegation) {
                logger.info("User {} is in Head Of Delegation subcommittee", user.getId());
            }

            return isHeadOfDelegation;
        } catch (Exception e) {
            logger.error("Error checking if user {} is Chair of Head Of Delegation: {}", user.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Get the Head Of Delegation subcommittee ID
     */
    public Long getHeadOfDelegationSubcommitteeId() {
        try {
            Optional<SubCommittee> hodSubcommittee = subCommitteeRepo.findByName(HEAD_OF_DELEGATION_SUBCOMMITTEE_NAME);
            if (hodSubcommittee.isPresent()) {
                return hodSubcommittee.get().getId();
            }
            logger.warn("Head Of Delegation subcommittee not found");
            return null;
        } catch (Exception e) {
            logger.error("Error getting Head Of Delegation subcommittee ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a subcommittee is the Head Of Delegation subcommittee
     */
    public boolean isHeadOfDelegationSubcommittee(Long subcommitteeId) {
        try {
            Optional<SubCommittee> subcommitteeOpt = subCommitteeRepo.findById(subcommitteeId);
            if (subcommitteeOpt.isEmpty()) {
                return false;
            }
            return HEAD_OF_DELEGATION_SUBCOMMITTEE_NAME.equals(subcommitteeOpt.get().getName());
        } catch (Exception e) {
            logger.error("Error checking if subcommittee {} is Head Of Delegation: {}", subcommitteeId, e.getMessage());
            return false;
        }
    }

    /**
     * Get user role display name considering HOD privileges
     */
    public String getUserRoleDisplay(User user) {
        if (hasHODPrivileges(user)) {
            // Only Chair/Vice Chair of Head of Delegation can have HOD privileges
            return "Head of Delegation";
        }
        return user.getRole().toString();
    }
}
