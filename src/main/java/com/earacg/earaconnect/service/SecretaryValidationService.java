package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Country;
import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.Meeting;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import com.earacg.earaconnect.repository.UserRepo;
import lombok.RequiredArgsConstructor;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretaryValidationService {

    private final UserRepo userRepo;
    private final CSubCommitteeMembersRepo cSubCommitteeMembersRepo;

    /**
     * Validate if a secretary can perform meeting-related tasks based on location
     * restrictions
     */
    public boolean validateSecretaryLocation(Long secretaryId, Meeting meeting) {
        try {
            User secretary = userRepo.findById(secretaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Secretary not found with ID: " + secretaryId));

            return validateSecretaryLocation(secretary, meeting);
        } catch (Exception e) {
            log.error("Error validating secretary location: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate if a secretary can manage a meeting based on their role and scope
     * (country or committee)
     */
    public boolean validateMeetingAccess(User secretary, Meeting meeting) {
        if (!isSecretary(secretary)) {
            log.warn("User {} is not a secretary", secretary.getEmail());
            return false;
        }

        User.UserRole role = secretary.getRole();

        // 1. Delegation Secretary or general Secretary: Country scope check
        if (role == User.UserRole.DELEGATION_SECRETARY || role == User.UserRole.SECRETARY) {
            if (secretary.getCountry() == null || meeting.getHostingCountry() == null) {
                return false;
            }
            return secretary.getCountry().getId().equals(meeting.getHostingCountry().getId());
        }

        // 2. Committee Secretary: Committee or Subcommittee scope check, or country from member record
        if (role == User.UserRole.COMMITTEE_SECRETARY) {
            Country effectiveCountry = getEffectiveCountryForCommitteeSecretary(secretary);
            log.info("COMMITTEE_SECRETARY validation: userId={}, email={}, userCountry={}, effectiveCountry={}, meetingHostCountry={}",
                    secretary.getId(), secretary.getEmail(),
                    secretary.getCountry() != null ? secretary.getCountry().getName() : null,
                    effectiveCountry != null ? effectiveCountry.getName() : null,
                    meeting.getHostingCountry() != null ? meeting.getHostingCountry().getName() : null);

            // When user has no subcommittee, allow only by country from member record
            if (secretary.getSubcommittee() == null) {
                if (effectiveCountry != null && meeting.getHostingCountry() != null
                        && effectiveCountry.getId().equals(meeting.getHostingCountry().getId())) {
                    return true;
                }
                log.warn("COMMITTEE_SECRETARY has no subcommittee; effectiveCountry or meeting host country mismatch");
                return false;
            }

            // Check if meeting is for the specific subcommittee
            if (meeting.getSubCommittee() != null &&
                    meeting.getSubCommittee().getId().equals(secretary.getSubcommittee().getId())) {
                return true;
            }

            // Check if meeting is for the parent committee
            if (meeting.getCommittee() != null &&
                    secretary.getSubcommittee().getParentCommittee() != null &&
                    meeting.getCommittee().getId().equals(secretary.getSubcommittee().getParentCommittee().getId())) {
                return true;
            }

            // Fallback: country match using member record (or user) when meeting not under their subcommittee/committee
            if (effectiveCountry != null && meeting.getHostingCountry() != null) {
                return effectiveCountry.getId().equals(meeting.getHostingCountry().getId());
            }
            return false;
        }

        // Fallback: If no specific rule matched, check country match as basic
        // requirement
        if (secretary.getCountry() != null && meeting.getHostingCountry() != null) {
            return secretary.getCountry().getId().equals(meeting.getHostingCountry().getId());
        }

        return false;
    }

    /**
     * For COMMITTEE_SECRETARY, prefer country from CSubCommitteeMembers (by email) so
     * validation matches the committee member record even if User.country is stale.
     * Uses normalized email lookup first (handles quotes/case/space in DB), then fallbacks.
     */
    private Country getEffectiveCountryForCommitteeSecretary(User secretary) {
        if (secretary.getEmail() == null || secretary.getEmail().isBlank()) {
            return secretary.getCountry();
        }
        String email = secretary.getEmail().trim();
        String emailNoQuotes = email;
        if (email.length() >= 2 && email.startsWith("\"") && email.endsWith("\"")) {
            emailNoQuotes = email.substring(1, email.length() - 1).trim();
        }
        // Prefer native normalized lookup (handles DB storing email with quotes/case/space)
        List<CSubCommitteeMembers> members = cSubCommitteeMembersRepo.findByEmailNormalized(emailNoQuotes);
        if (members.isEmpty()) {
            members = cSubCommitteeMembersRepo.findByEmailIgnoreCase(emailNoQuotes);
        }
        if (members.isEmpty()) {
            members = cSubCommitteeMembersRepo.findByEmail(emailNoQuotes);
        }
        if (members.isEmpty() && !emailNoQuotes.equals(email)) {
            members = cSubCommitteeMembersRepo.findByEmailIgnoreCase(email);
        }
        if (members.isEmpty()) {
            members = cSubCommitteeMembersRepo.findByEmailIgnoreCase("\"" + emailNoQuotes + "\"");
        }
        if (members.isEmpty()) {
            members = cSubCommitteeMembersRepo.findByEmail("\"" + emailNoQuotes + "\"");
        }
        log.info("getEffectiveCountryForCommitteeSecretary: userEmail='{}', normalized='{}', membersFound={}", secretary.getEmail(), emailNoQuotes, members.size());
        for (CSubCommitteeMembers m : members) {
            if (!m.isCommitteeSecretary()) {
                continue;
            }
            if (secretary.getSubcommittee() != null && m.getSubCommittee() != null
                    && m.getSubCommittee().getId().equals(secretary.getSubcommittee().getId())
                    && m.getCountry() != null) {
                log.debug("Using country from member (subcommittee match): {}", m.getCountry().getName());
                return m.getCountry();
            }
        }
        for (CSubCommitteeMembers m : members) {
            if (m.isCommitteeSecretary() && m.getCountry() != null) {
                log.debug("Using country from member (any committee secretary): {}", m.getCountry().getName());
                return m.getCountry();
            }
        }
        log.debug("No member record with country found; using user country: {}", secretary.getCountry() != null ? secretary.getCountry().getName() : null);
        return secretary.getCountry();
    }

    /**
     * Validate if a secretary can perform meeting-related tasks based on location
     * restrictions
     * Deprecated: Use validateMeetingAccess instead
     */
    @Deprecated
    public boolean validateSecretaryLocation(User secretary, Meeting meeting) {
        return validateMeetingAccess(secretary, meeting);
    }

    /**
     * Check if user has secretary role
     */
    public boolean isSecretary(User user) {
        return user.getRole() == User.UserRole.SECRETARY ||
                user.getRole() == User.UserRole.COMMITTEE_SECRETARY ||
                user.getRole() == User.UserRole.DELEGATION_SECRETARY;
    }

    /**
     * Validate secretary can create meeting in specific country.
     * For COMMITTEE_SECRETARY uses effective country from CSubCommitteeMembers (by email).
     */
    public boolean validateSecretaryCanCreateMeeting(Long secretaryId, Long hostingCountryId) {
        try {
            User secretary = userRepo.findById(secretaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Secretary not found with ID: " + secretaryId));

            if (!isSecretary(secretary)) {
                return false;
            }

            if (secretary.getRole() == User.UserRole.COMMITTEE_SECRETARY) {
                Country effective = getEffectiveCountryForCommitteeSecretary(secretary);
                if (effective == null) {
                    log.warn("Committee secretary {} has no country (user or member record)", secretary.getEmail());
                    return false;
                }
                return effective.getId().equals(hostingCountryId);
            }

            if (secretary.getCountry() == null) {
                log.warn("Secretary {} has no country assigned", secretary.getEmail());
                return false;
            }
            return secretary.getCountry().getId().equals(hostingCountryId);
        } catch (Exception e) {
            log.error("Error validating secretary meeting creation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get validation message for location restrictions. Role-aware so committee
     * secretaries see scope-based messages instead of only country mismatch.
     */
    public String getLocationValidationMessage(User secretary, Meeting meeting) {
        if (!isSecretary(secretary)) {
            return "Only secretaries can perform meeting management tasks";
        }

        User.UserRole role = secretary.getRole();

        // Committee secretary: prefer scope-based messages; use effective country from member record when relevant
        if (role == User.UserRole.COMMITTEE_SECRETARY) {
            Country effective = getEffectiveCountryForCommitteeSecretary(secretary);
            if (secretary.getSubcommittee() == null) {
                if (effective != null && meeting.getHostingCountry() != null
                        && !effective.getId().equals(meeting.getHostingCountry().getId())) {
                    return String.format("Secretary from %s cannot manage meetings hosted in %s",
                            effective.getName(), meeting.getHostingCountry().getName());
                }
                return "Committee secretary must have a subcommittee or a committee member record with country assigned to manage meetings";
            }
            boolean meetingHasSub = meeting.getSubCommittee() != null;
            boolean meetingHasCommittee = meeting.getCommittee() != null;
            if (!meetingHasSub && !meetingHasCommittee) {
                if (effective != null && meeting.getHostingCountry() != null
                        && !effective.getId().equals(meeting.getHostingCountry().getId())) {
                    return String.format("Secretary from %s cannot manage meetings hosted in %s",
                            effective.getName(), meeting.getHostingCountry().getName());
                }
                return "This meeting is not assigned to a subcommittee or committee. You can only manage meetings under your subcommittee or committee.";
            }
            return "You can only manage meetings for your subcommittee or committee. This meeting is not under your scope.";
        }

        // Delegation / general secretary: country-based message
        if (secretary.getCountry() == null) {
            return "Secretary must have a country assigned to manage meetings";
        }
        if (meeting.getHostingCountry() == null) {
            return "Meeting must have a hosting country assigned";
        }
        if (!secretary.getCountry().getId().equals(meeting.getHostingCountry().getId())) {
            return String.format("Secretary from %s cannot manage meetings hosted in %s",
                    secretary.getCountry().getName(),
                    meeting.getHostingCountry().getName());
        }
        return "Location validation passed";
    }

    /**
     * Validate secretary can take minutes for a meeting
     */
    public ValidationResult validateMinuteTaking(Long secretaryId, Long meetingId) {
        try {
            User secretary = userRepo.findById(secretaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Secretary not found"));

            // For now, we'll just validate the secretary role and location
            // In a real system, you might also check if the meeting is in progress
            if (!isSecretary(secretary)) {
                return new ValidationResult(false, "Only secretaries can take meeting minutes");
            }

            if (secretary.getRole() == User.UserRole.COMMITTEE_SECRETARY) {
                if (getEffectiveCountryForCommitteeSecretary(secretary) == null && secretary.getCountry() == null) {
                    return new ValidationResult(false, "Committee secretary must have a country assigned (in profile or committee member record)");
                }
            } else if (secretary.getCountry() == null) {
                return new ValidationResult(false, "Secretary must have a country assigned");
            }

            return new ValidationResult(true, "Secretary authorized to take minutes");
        } catch (Exception e) {
            log.error("Error validating minute taking: {}", e.getMessage());
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * Validate secretary can assign resolutions
     */
    public ValidationResult validateResolutionAssignment(Long secretaryId) {
        try {
            User secretary = userRepo.findById(secretaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Secretary not found"));

            if (!isSecretary(secretary)) {
                return new ValidationResult(false, "Only secretaries can assign resolutions");
            }

            return new ValidationResult(true, "Secretary authorized to assign resolutions");
        } catch (Exception e) {
            log.error("Error validating resolution assignment: {}", e.getMessage());
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * NEW WORKFLOW: Validate if secretary can create resolutions for a specific meeting type
     * RULE: Only Delegation Secretary can create resolutions, and only for CG meetings
     */
    public ValidationResult validateResolutionCreationForMeetingType(User secretary, Meeting meeting) {
        try {
            if (!isSecretary(secretary)) {
                return new ValidationResult(false, "Only secretaries can create resolutions");
            }

            User.UserRole role = secretary.getRole();

            // Committee Secretary cannot create resolutions (they only describe tasks)
            if (role == User.UserRole.COMMITTEE_SECRETARY) {
                return new ValidationResult(false, 
                    "Committee Secretary cannot create resolutions. Committee Secretaries describe tasks from Technical Committee meetings.");
            }

            // Delegation Secretary and general Secretary can create resolutions
            if (role == User.UserRole.DELEGATION_SECRETARY || role == User.UserRole.SECRETARY) {
                // Only for Commissioner General meetings
                if (meeting.getMeetingType() != Meeting.MeetingType.COMMISSIONER_GENERAL_MEETING) {
                    return new ValidationResult(false, 
                        "Resolutions can only be created for Commissioner General meetings. For Technical Committee meetings, create tasks instead.");
                }
                return new ValidationResult(true, "Delegation Secretary authorized to create resolutions for CG meeting");
            }

            return new ValidationResult(false, "Invalid secretary role for resolution creation");
        } catch (Exception e) {
            log.error("Error validating resolution creation for meeting type: {}", e.getMessage());
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * NEW WORKFLOW: Validate if secretary can create tasks for a specific meeting type
     * RULE: 
     * - Delegation Secretary can create tasks ONLY for Technical Committee meetings (title only, no description/deadline)
     * - Committee Secretary CANNOT create new tasks (they only describe existing TC tasks)
     */
    public ValidationResult validateTaskCreationForMeetingType(User secretary, Meeting meeting) {
        try {
            if (!isSecretary(secretary)) {
                return new ValidationResult(false, "Only secretaries can create tasks");
            }

            User.UserRole role = secretary.getRole();

            // Committee Secretary cannot create new tasks
            if (role == User.UserRole.COMMITTEE_SECRETARY) {
                return new ValidationResult(false, 
                    "Committee Secretary cannot create new tasks. You can only add descriptions to existing tasks from Technical Committee meetings.");
            }

            // Delegation Secretary and general Secretary can create tasks
            if (role == User.UserRole.DELEGATION_SECRETARY || role == User.UserRole.SECRETARY) {
                // Only for Technical Committee meetings
                if (meeting.getMeetingType() != Meeting.MeetingType.TECHNICAL_MEETING) {
                    return new ValidationResult(false, 
                        "Tasks can only be created for Technical Committee meetings. For Commissioner General meetings, create resolutions instead.");
                }
                return new ValidationResult(true, "Delegation Secretary authorized to create tasks for TC meeting");
            }

            return new ValidationResult(false, "Invalid secretary role for task creation");
        } catch (Exception e) {
            log.error("Error validating task creation for meeting type: {}", e.getMessage());
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * NEW WORKFLOW: Validate if Committee Secretary can update task description
     * RULE: Only Committee Secretary can add descriptions to TC tasks that have requiresDescription = true
     */
    public ValidationResult validateTaskDescriptionUpdate(User secretary, com.earacg.earaconnect.model.SubTask task) {
        try {
            if (!isSecretary(secretary)) {
                return new ValidationResult(false, "Only secretaries can update task descriptions");
            }

            User.UserRole role = secretary.getRole();

            // Only Committee Secretary can update task descriptions
            if (role != User.UserRole.COMMITTEE_SECRETARY) {
                return new ValidationResult(false, 
                    "Only Committee Secretary can add descriptions to tasks. Delegation Secretary creates title-only tasks.");
            }

            // Check if task requires description
            if (task.getRequiresDescription() == null || !task.getRequiresDescription()) {
                return new ValidationResult(false, 
                    "This task does not require description update. It may already have a description or was not created from a Technical Committee meeting.");
            }

            // Check if task is from Technical Committee meeting
            if (!"TECHNICAL_MEETING".equals(task.getSourceMeetingType())) {
                return new ValidationResult(false, 
                    "Only tasks from Technical Committee meetings can have descriptions added.");
            }

            // Check if secretary's subcommittee matches task's subcommittee
            if (secretary.getSubcommittee() == null) {
                return new ValidationResult(false, 
                    "Committee Secretary must have an assigned subcommittee to update task descriptions.");
            }

            if (!secretary.getSubcommittee().getId().equals(task.getSubcommitteeId())) {
                return new ValidationResult(false, 
                    "You can only update descriptions for tasks assigned to your subcommittee.");
            }

            return new ValidationResult(true, "Committee Secretary authorized to update task description");
        } catch (Exception e) {
            log.error("Error validating task description update: {}", e.getMessage());
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    /**
     * Result class for validation operations
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public boolean getValid() {
            return valid;
        }
    }
}
