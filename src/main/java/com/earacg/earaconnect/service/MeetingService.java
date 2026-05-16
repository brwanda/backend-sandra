package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.*;
import com.earacg.earaconnect.repository.*;
import com.earacg.earaconnect.controller.MeetingController.AttendanceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class MeetingService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private MeetingInvitationRepo meetingInvitationRepo;

    @Autowired
    private MeetingRepo meetingRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SecretaryValidationService secretaryValidationService;

    @Autowired
    private CountryRepo countryRepo;

    @Autowired
    private CommitteeRepo committeeRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    @Autowired
    private EntityHistoryService entityHistoryService;

    // Basic CRUD Operations
    public List<Meeting> getAllMeetings() {
        return meetingRepo.findAll();
    }

    public Optional<Meeting> getMeetingById(Long id) {
        return meetingRepo.findById(id);
    }

    public List<Meeting> getMeetingsByCreator(Long createdById) {
        return meetingRepo.findByCreatedById(createdById);
    }

    public List<Meeting> getMeetingsByCountry(Long countryId) {
        return meetingRepo.findByHostingCountryId(countryId);
    }

    public List<Meeting> getMeetingsByType(Meeting.MeetingType meetingType) {
        return meetingRepo.findByMeetingType(meetingType);
    }

    public Meeting createMeeting(Meeting meeting, User creator) {
        System.out.println("🚀 MeetingService.createMeeting called for Title: " + meeting.getTitle());

        // 1. Validate required fields (fail early to avoid hibernate 500s)
        if (meeting.getTitle() == null || meeting.getTitle().trim().isEmpty()) {
            System.err.println("❌ Validation Fail: Title is empty");
            throw new IllegalArgumentException("Meeting title is required");
        }
        if (meeting.getMeetingDate() == null) {
            System.err.println("❌ Validation Fail: MeetingDate is null");
            throw new IllegalArgumentException("Meeting date is required");
        }
        if (meeting.getMeetingEndDate() != null && meeting.getMeetingEndDate().isBefore(meeting.getMeetingDate())) {
            throw new IllegalArgumentException("Meeting end date cannot be before start date");
        }
        if (meeting.getHostingCountry() == null || meeting.getHostingCountry().getId() == null) {
            System.err.println("❌ Validation Fail: HostingCountry or ID is null");
            throw new IllegalArgumentException("Hosting country is required");
        }
        if (creator == null) {
            System.err.println("❌ Validation Fail: Creator is null");
            throw new IllegalArgumentException("Authenticated user is required to create meeting");
        }

        // 2. Fetch full entity objects to ensure they exist and prevent NPEs later
        User fullCreator = userRepo.findById(creator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with ID: " + creator.getId()));

        if (fullCreator.getRole() == User.UserRole.COMMITTEE_SECRETARY) {
            if (fullCreator.getSubcommittee() == null || fullCreator.getSubcommittee().getId() == null) {
                throw new IllegalArgumentException("Committee Secretary must have an assigned subcommittee.");
            }

            if (meeting.getMeetingType() != null && meeting.getMeetingType() != Meeting.MeetingType.SUBCOMMITTEE_MEETING) {
                throw new IllegalArgumentException("Committee Secretary meetings must be subcommittee meetings.");
            }

            if (meeting.getSubCommittee() != null
                    && meeting.getSubCommittee().getId() != null
                    && !fullCreator.getSubcommittee().getId().equals(meeting.getSubCommittee().getId())) {
                throw new IllegalArgumentException(
                        "Committee Secretary can only create meetings for their assigned subcommittee.");
            }

            meeting.setMeetingType(Meeting.MeetingType.SUBCOMMITTEE_MEETING);
            meeting.setSubCommittee(fullCreator.getSubcommittee());
        }

        if (meeting.getMeetingType() == null) {
            System.err.println("❌ Validation Fail: MeetingType is null");
            throw new IllegalArgumentException("Meeting type is required");
        }

        Country hostingCountry = countryRepo.findById(meeting.getHostingCountry().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hosting country not found with ID: " + meeting.getHostingCountry().getId()));

        meeting.setCreatedBy(fullCreator);
        meeting.setHostingCountry(hostingCountry);

        if (meeting.getCommittee() != null && meeting.getCommittee().getId() != null) {
            Committee committee = committeeRepo.findById(meeting.getCommittee().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Committee not found with ID: " + meeting.getCommittee().getId()));
            meeting.setCommittee(committee);
        }

        if (meeting.getSubCommittee() != null && meeting.getSubCommittee().getId() != null) {
            SubCommittee subCommittee = subCommitteeRepo.findById(meeting.getSubCommittee().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subcommittee not found with ID: " + meeting.getSubCommittee().getId()));
            meeting.setSubCommittee(subCommittee);
            System.out.println("✅ Subcommittee linked: " + subCommittee.getName());
        }

        // 3. Authorization & Business Logic Validation
        if (!secretaryValidationService.isSecretary(fullCreator)) {
            throw new IllegalArgumentException("Only secretaries are authorized to create meetings");
        }

        validateCreatorMeetingScope(fullCreator, meeting);

        Meeting.MeetingMode mode = meeting.getMeetingMode() != null ? meeting.getMeetingMode() : Meeting.MeetingMode.PHYSICAL;
        meeting.setMeetingMode(mode);
        if (mode == Meeting.MeetingMode.ONLINE) {
            if (meeting.getMeetingLink() == null || meeting.getMeetingLink().isBlank()) {
                throw new IllegalArgumentException("Meeting link is required for online meetings");
            }
            meeting.setLocation(null);
        } else {
            if (meeting.getLocation() == null || meeting.getLocation().isBlank()) {
                throw new IllegalArgumentException("Location is required for physical meetings");
            }
            meeting.setMeetingLink(null);
        }

        // STRICTOR VALIDATION: Meeting date must be in the future
        if (!meeting.getMeetingDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Meeting date must be in the future (after " + LocalDateTime.now() + ")");
        }

        if (!secretaryValidationService.validateMeetingAccess(fullCreator, meeting)) {
            String message = secretaryValidationService.getLocationValidationMessage(fullCreator, meeting);
            throw new IllegalArgumentException("Permission denied: " + message);
        }

        // 4. Persistence
        if (meeting.getMeetingEndDate() == null) {
            meeting.setMeetingEndDate(meeting.getMeetingDate());
        }
        meeting.setStatus(Meeting.MeetingStatus.SCHEDULED);
        Meeting savedMeeting = meetingRepo.save(meeting);

        // 5. Invitations
        boolean shouldSendNotifications = meeting.getSendNotifications() == null || meeting.getSendNotifications();
        if (shouldSendNotifications) {
            try {
                sendMeetingInvitations(savedMeeting);
            } catch (Exception e) {
                // Log but don't fail the whole creation if emails crash
                System.err.println("Failed to send automatic invitations: " + e.getMessage());
            }
        }

        return savedMeeting;
    }

    private void validateCreatorMeetingScope(User creator, Meeting meeting) {
        User.UserRole role = creator.getRole();
        Meeting.MeetingType meetingType = meeting.getMeetingType();

        if (role == User.UserRole.DELEGATION_SECRETARY || role == User.UserRole.SECRETARY) {
            boolean allowed = meetingType == Meeting.MeetingType.COMMISSIONER_GENERAL_MEETING
                    || meetingType == Meeting.MeetingType.TECHNICAL_MEETING;
            if (!allowed) {
                throw new IllegalArgumentException(
                        "Delegation Secretary can create only Commissioner General and Technical Committee meetings.");
            }
            return;
        }

        if (role == User.UserRole.COMMITTEE_SECRETARY) {
            if (meetingType != Meeting.MeetingType.SUBCOMMITTEE_MEETING) {
                throw new IllegalArgumentException(
                        "Committee Secretary can create only Subcommittee meetings for their assigned subcommittee.");
            }
            if (creator.getSubcommittee() == null || creator.getSubcommittee().getId() == null) {
                throw new IllegalArgumentException("Committee Secretary must belong to an assigned subcommittee.");
            }
            if (meeting.getSubCommittee() == null || meeting.getSubCommittee().getId() == null) {
                throw new IllegalArgumentException("Subcommittee is required for subcommittee meetings.");
            }

            SubCommittee targetSubcommittee = subCommitteeRepo.findById(meeting.getSubCommittee().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Subcommittee not found"));

            if (!creator.getSubcommittee().getId().equals(targetSubcommittee.getId())) {
                throw new IllegalArgumentException(
                        "Committee Secretary can create meetings only for their assigned subcommittee.");
            }
        }
    }

    public Meeting uploadInvitationPdf(Long meetingId, MultipartFile invitationPdf) {
        Optional<Meeting> meetingOpt = meetingRepo.findById(meetingId);
        if (meetingOpt.isPresent()) {
            Meeting meeting = meetingOpt.get();

            try {
                // Generate unique filename
                String fileName = "invitation_" + System.currentTimeMillis() + "_"
                        + invitationPdf.getOriginalFilename();
                // Store file path (you might want to implement actual file storage)
                meeting.setInvitationPdf(fileName);
                return meetingRepo.save(meeting);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process invitation PDF", e);
            }
        }
        return null;
    }

    public Meeting updateMeeting(Long id, Meeting meetingDetails) {
        Optional<Meeting> meetingOpt = meetingRepo.findById(id);
        if (meetingOpt.isPresent()) {
            Meeting meeting = meetingOpt.get();
            meeting.setTitle(meetingDetails.getTitle());
            meeting.setDescription(meetingDetails.getDescription());
            meeting.setAgenda(meetingDetails.getAgenda());
            meeting.setMeetingDate(meetingDetails.getMeetingDate());
            meeting.setLocation(meetingDetails.getLocation());
            meeting.setMeetingType(meetingDetails.getMeetingType());
            meeting.setStatus(meetingDetails.getStatus());
            meeting.setMinutes(meetingDetails.getMinutes());
            return meetingRepo.save(meeting);
        }
        return null;
    }

    public boolean deleteMeeting(Long id) {
        java.util.Optional<com.earacg.earaconnect.model.Meeting> meetingOpt = meetingRepo.findById(id);
        if (meetingOpt.isPresent()) {
            entityHistoryService.recordDeletion("Meeting", id, meetingOpt.get(), null, null);
            meetingRepo.deleteById(id);
            return true;
        }
        return false;
    }

    // Secretary validation
    public boolean validateSecretaryLocation(Long meetingId, Long secretaryId) {
        Optional<Meeting> meetingOpt = meetingRepo.findById(meetingId);
        if (meetingOpt.isPresent()) {
            Meeting meeting = meetingOpt.get();

            Optional<User> userOpt = userRepo.findById(secretaryId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Use the refined multi-scope validation
                return secretaryValidationService.validateMeetingAccess(user, meeting);
            }
        }
        return false;
    }

    // Invitation Management

    /**
     * Send automatic invitations based on meeting type (used during meeting
     * creation)
     */
    private void sendMeetingInvitations(Meeting meeting) {
        List<User> usersToInvite = getUsersForMeetingAudience(meeting);

        for (User user : usersToInvite) {
            createAndSendInvitation(meeting, user);
        }
    }

    /**
     * Send invitations to specific users for a meeting (secretary-initiated)
     */
    public void sendInvitationsToUsers(Long meetingId, List<Long> userIds) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        System.out.println("📧 Starting to send invitations for meeting: " + meeting.getTitle());
        System.out.println("👥 Number of users to invite: " + userIds.size());

        int successCount = 0;
        int errorCount = 0;

        for (Long userId : userIds) {
            try {
                User user = userRepo.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

                // Check if invitation already exists
                Optional<MeetingInvitation> existingInvitation = meetingInvitationRepo
                        .findByMeetingIdAndUserId(meetingId, userId);

                if (existingInvitation.isEmpty()) {
                    createAndSendInvitation(meeting, user);
                    successCount++;
                    System.out.println(
                            "✅ Invitation created and sent for user: " + user.getName() + " (" + user.getEmail() + ")");
                } else {
                    System.out.println(
                            "⚠️ Invitation already exists for user: " + user.getName() + " (" + user.getEmail() + ")");
                    successCount++; // Count as success since invitation exists
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("❌ Failed to process invitation for user ID " + userId + ": " + e.getMessage());
            }
        }

        System.out.println("📊 Invitation summary - Success: " + successCount + ", Errors: " + errorCount);
    }

    /**
     * Helper method to create and send invitation
     */
    private void createAndSendInvitation(Meeting meeting, User user) {
        // Create new invitation
        MeetingInvitation invitation = new MeetingInvitation();
        invitation.setMeeting(meeting);
        invitation.setUser(user);
        invitation.setStatus(MeetingInvitation.InvitationStatus.PENDING);
        invitation.setSentAt(LocalDateTime.now());

        // Save invitation
        meetingInvitationRepo.save(invitation);

        // Send email invitation with error handling
        try {
            System.out.println("📧 Attempting to send email invitation to: " + user.getEmail());
            emailService.sendMeetingInvitation(
                    user.getEmail(),
                    user.getName(),
                    meeting.getTitle(),
                    meeting.getMeetingDate().toString(),
                    meeting.getLocation());
            System.out.println("✅ Email invitation sent successfully to: " + user.getEmail());
        } catch (Exception emailError) {
            System.err.println(
                    "❌ Failed to send email invitation to " + user.getEmail() + ": " + emailError.getMessage());
            // Don't throw the exception - continue with the invitation process
            // The invitation is still created in the database
        }

        // Create in-system notification
        try {
            notificationService.createNotification(
                    user.getId(),
                    "Meeting Invitation",
                    "You have been invited to attend: " + meeting.getTitle(),
                    Notification.NotificationType.MEETING_INVITATION,
                    "Meeting",
                    meeting.getId());
        } catch (Exception notificationError) {
            System.err.println(
                    "❌ Failed to create notification for user " + user.getId() + ": " + notificationError.getMessage());
            // Don't throw the exception - continue with the invitation process
        }
    }

    /**
     * Get users to invite based on meeting type and country (REMOVED BOARD_MEMBER)
     */
    private List<User> getUsersForMeetingAudience(Meeting meeting) {
        Meeting.MeetingType meetingType = meeting.getMeetingType();
        Long countryId = meeting.getHostingCountry() != null ? meeting.getHostingCountry().getId() : null;

        switch (meetingType) {
            case COMMISSIONER_GENERAL_MEETING:
                return userRepo.findByActive(true).stream()
                        .filter(u -> u.getRole() != User.UserRole.ADMIN)
                        .collect(Collectors.toList());
            case TECHNICAL_MEETING:
                return userRepo.findByActive(true).stream()
                        .filter(u -> u.getRole() != User.UserRole.ADMIN)
                        .filter(u -> u.getRole() != User.UserRole.COMMISSIONER_GENERAL)
                        .collect(Collectors.toList());
            case SUBCOMMITTEE_MEETING:
                if (meeting.getSubCommittee() != null && meeting.getSubCommittee().getId() != null) {
                    SubCommittee currentSubcommittee = subCommitteeRepo.findById(meeting.getSubCommittee().getId()).orElse(null);
                    if (currentSubcommittee != null && currentSubcommittee.getParentCommittee() != null) {
                        Long parentCommitteeId = currentSubcommittee.getParentCommittee().getId();
                        List<SubCommittee> allSubcommittees = subCommitteeRepo.findAll();
                        Set<Long> subcommitteeIdsInParent = allSubcommittees.stream()
                                .filter(sc -> sc.getParentCommittee() != null && parentCommitteeId.equals(sc.getParentCommittee().getId()))
                                .map(SubCommittee::getId)
                                .collect(Collectors.toSet());

                        return userRepo.findByActive(true).stream()
                                .filter(u -> u.getRole() != User.UserRole.ADMIN)
                                .filter(u -> u.getSubcommittee() != null && subcommitteeIdsInParent.contains(u.getSubcommittee().getId()))
                                .collect(Collectors.toList());
                    }
                }
                return List.of();
            default:
                if (countryId == null) {
                    return List.of();
                }
                return userRepo.findByCountryId(countryId);
        }
    }

    // Attendance Management

    /**
     * Record attendance for a meeting
     */
    @Transactional
    public void recordAttendance(Long meetingId, List<AttendanceRecord> attendanceRecords) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        for (AttendanceRecord record : attendanceRecords) {
            User user = userRepo.findById(record.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + record.getUserId()));

            // Check if attendance record already exists
            Optional<Attendance> existingAttendance = attendanceRepo
                    .findByMeetingIdAndUserId(meetingId, record.getUserId());

            if (existingAttendance.isPresent()) {
                // Update existing attendance
                Attendance attendance = existingAttendance.get();
                attendance.setStatus(Attendance.AttendanceStatus.valueOf(record.getStatus().toUpperCase()));
                attendance.setNotes(record.getNotes());
                attendance.setRecordedAt(LocalDateTime.now());
                attendanceRepo.save(attendance);
            } else {
                // Create new attendance record
                Attendance attendance = new Attendance();
                attendance.setMeeting(meeting);
                attendance.setUser(user);
                attendance.setStatus(Attendance.AttendanceStatus.valueOf(record.getStatus().toUpperCase()));
                attendance.setNotes(record.getNotes());
                attendance.setRecordedAt(LocalDateTime.now());
                attendanceRepo.save(attendance);
            }
        }
    }

    /**
     * Get attendance records for a meeting
     */
    public List<AttendanceRecord> getAttendance(Long meetingId) {
        List<Attendance> attendanceList = attendanceRepo.findByMeetingId(meetingId);

        return attendanceList.stream()
                .map(attendance -> new AttendanceRecord(
                        attendance.getUser().getId(),
                        attendance.getStatus().toString(),
                        attendance.getNotes()))
                .collect(Collectors.toList());
    }

    // Minutes Management
    public Meeting updateMeetingMinutes(Long id, String minutes) {
        System.out.println("📝 MeetingService.updateMeetingMinutes - Attempting to save minutes for meeting ID: " + id);

        Optional<Meeting> meetingOpt = meetingRepo.findById(id);
        if (!meetingOpt.isPresent()) {
            System.err.println("❌ MeetingService.updateMeetingMinutes - Meeting NOT FOUND with ID: " + id);
            throw new RuntimeException("Meeting not found with ID: " + id);
        }
        Meeting meeting = meetingOpt.get();

        // Relaxed Time Gate: Allow recording minutes even if meeting is in the future
        // (preparation mode)
        if (meeting.getMeetingDate() != null && meeting.getMeetingDate().isAfter(LocalDateTime.now())) {
            System.out.println("⚠️ Preparation Mode: Recording minutes for a future meeting scheduled for: "
                    + meeting.getMeetingDate());
            // We'll let it pass now as per user request to be able to "prepare"
        }

        meeting.setMinutes(minutes);
        // If minutes are saved, we transition to COMPLETED (or PENDING_MINUTES if
        // partial? No, standard is COMPLETED)
        meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
        meeting.setUpdatedAt(LocalDateTime.now());

        System.out.println("💾 Saving meeting minutes. Status changed to: " + meeting.getStatus());
        Meeting saved = meetingRepo.save(meeting);
        System.out.println("✅ Meeting minutes saved successfully for ID: " + saved.getId());
        return saved;
    }

    /** Get resolutions for a specific meeting */
    public List<Resolution> getResolutionsForMeeting(Long meetingId) {
        if (!meetingRepo.existsById(meetingId)) {
            throw new RuntimeException("Meeting not found");
        }
        return resolutionRepo.findByMeetingId(meetingId);
    }

    // Task Management
    @Autowired
    private SubTaskRepository subTaskRepository;

    public void createTasks(Long meetingId, Map<String, Object> request) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        List<Map<String, Object>> tasksData = (List<Map<String, Object>>) request.get("tasks");
        if (tasksData == null || tasksData.isEmpty()) {
            throw new RuntimeException("No tasks provided");
        }

        User.UserRole creatorRole = meeting.getCreatedBy() != null ? meeting.getCreatedBy().getRole() : null;
        boolean delegationSecretaryFlow = creatorRole == User.UserRole.DELEGATION_SECRETARY;
        boolean commissionerGeneralMeeting = meeting.getMeetingType() == Meeting.MeetingType.COMMISSIONER_GENERAL_MEETING;
        boolean technicalMeeting = meeting.getMeetingType() == Meeting.MeetingType.TECHNICAL_MEETING;
        boolean createdAnyTask = false;

        if (delegationSecretaryFlow && commissionerGeneralMeeting) {
            throw new IllegalArgumentException(
                    "Delegation Secretary Commissioner General meetings must proceed with resolution creation, not direct task creation.");
        }

        for (Map<String, Object> taskData : tasksData) {
            String title = taskData.get("title") != null ? taskData.get("title").toString().trim() : "";
            String description = taskData.get("description") != null ? taskData.get("description").toString().trim() : "";

            if (delegationSecretaryFlow && technicalMeeting) {
                if (title.isBlank()) {
                    throw new IllegalArgumentException("Task title is required for Delegation Secretary technical meetings.");
                }
                if (!description.isBlank()) {
                    throw new IllegalArgumentException(
                            "Delegation Secretary technical meeting tasks must use title only. Remove task description.");
                }
            }

            SubTask task = new SubTask();
            task.setTitle(title.isBlank() ? null : title);
            task.setDescription(description.isBlank() ? null : description);
            task.setMeeting(meeting);

            // Link to resolution if provided
            if (taskData.get("resolutionId") != null) {
                Long resolutionId = Long.valueOf(taskData.get("resolutionId").toString());
                Resolution resolution = resolutionRepo.findById(resolutionId)
                        .orElseThrow(() -> new RuntimeException("Resolution not found with ID: " + resolutionId));
                task.setResolution(resolution);
            }

            // Set subcommittee if provided
            if (taskData.get("subcommitteeId") != null) {
                task.setSubcommitteeId(Long.valueOf(taskData.get("subcommitteeId").toString()));
            } else if (meeting.getSubCommittee() != null) {
                task.setSubcommitteeId(meeting.getSubCommittee().getId());
            }

            task.setStatus(SubTask.TaskStatus.TODO);
            task.setCreatedAt(LocalDateTime.now());

            subTaskRepository.save(task);
            createdAnyTask = true;
        }

        if (delegationSecretaryFlow && technicalMeeting && !createdAnyTask) {
            throw new IllegalArgumentException("At least one task is required for Delegation Secretary technical meetings.");
        }
    }

    @Autowired
    private ResolutionAssignmentRepo resolutionAssignmentRepo;

    // Resolution Management (Simplified)
    @Transactional
    public void createResolutions(Long meetingId, Map<String, Object> request) {
        Optional<Meeting> meetingOpt = meetingRepo.findById(meetingId);
        if (!meetingOpt.isPresent()) {
            throw new RuntimeException("Meeting not found");
        }

        Meeting meeting = meetingOpt.get();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resolutionsData = (List<Map<String, Object>>) request.get("resolutions");

        if (resolutionsData == null || resolutionsData.isEmpty()) {
            throw new RuntimeException("No resolutions provided");
        }

        User.UserRole creatorRole = meeting.getCreatedBy() != null ? meeting.getCreatedBy().getRole() : null;
        boolean delegationSecretaryFlow = creatorRole == User.UserRole.DELEGATION_SECRETARY
                || creatorRole == User.UserRole.SECRETARY;
        boolean strictDelegationSecretaryFlow = creatorRole == User.UserRole.DELEGATION_SECRETARY;
        boolean committeeSecretaryFlow = creatorRole == User.UserRole.COMMITTEE_SECRETARY;
        boolean commissionerGeneralMeeting = meeting.getMeetingType() == Meeting.MeetingType.COMMISSIONER_GENERAL_MEETING;
        boolean technicalMeeting = meeting.getMeetingType() == Meeting.MeetingType.TECHNICAL_MEETING;
        boolean subcommitteeMeeting = meeting.getMeetingType() == Meeting.MeetingType.SUBCOMMITTEE_MEETING;

        if (strictDelegationSecretaryFlow && technicalMeeting) {
            throw new IllegalArgumentException(
                "Delegation Secretary technical meetings must proceed with task creation only.");
        }

        for (Map<String, Object> resolutionData : resolutionsData) {
            String resolutionTitle = resolutionData.get("title") != null ? resolutionData.get("title").toString().trim() : "";
            if (resolutionTitle.isBlank()) {
                throw new IllegalArgumentException("Resolution title is required");
            }

            String resolutionDescription = resolutionData.get("description") != null
                    ? resolutionData.get("description").toString().trim()
                    : "";

            if (delegationSecretaryFlow && commissionerGeneralMeeting && !resolutionDescription.isBlank()) {
                throw new IllegalArgumentException(
                        "Delegation Secretary Commissioner General resolutions must use title only.");
            }

            Resolution resolution = new Resolution();
            resolution.setTitle(resolutionTitle);
            resolution.setDescription(resolutionDescription.isBlank() ? null : resolutionDescription);
            resolution.setMeeting(meeting);
            resolution.setCreatedBy(meeting.getCreatedBy());
            resolution.setStatus(Resolution.ResolutionStatus.ASSIGNED);
            resolution.setCreatedAt(LocalDateTime.now());

            // Save resolution first to get its ID
            Resolution savedResolution = resolutionRepo.save(resolution);

            // Process tasks within this resolution
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasksData = (List<Map<String, Object>>) resolutionData.get("tasks");

            if (delegationSecretaryFlow && commissionerGeneralMeeting && tasksData != null) {
                boolean hasAnyTask = tasksData.stream().anyMatch(task -> {
                    Object titleObj = task.get("title");
                    return titleObj != null && !titleObj.toString().trim().isBlank();
                });
                if (hasAnyTask) {
                    throw new IllegalArgumentException(
                            "Delegation Secretary Commissioner General meetings can only create resolution titles. Tasks are not allowed.");
                }
            }

            if (tasksData != null && !tasksData.isEmpty()) {
                // Collect unique subcommittee IDs for creating ResolutionAssignment records
                java.util.LinkedHashSet<Long> assignedSubcommitteeIds = new java.util.LinkedHashSet<>();
                boolean createdAnyTask = false;

                for (Map<String, Object> taskData : tasksData) {
                    String taskTitle = (String) taskData.get("title");
                    if (taskTitle == null || taskTitle.trim().isEmpty()) continue;

                    String taskDescription = taskData.get("description") != null
                            ? taskData.get("description").toString().trim()
                            : "";
                    LocalDateTime taskDeadline = parseDeadlineValue(taskData.get("deadline"));

                    if (delegationSecretaryFlow && technicalMeeting) {
                        if (!taskDescription.isBlank() || taskDeadline != null) {
                            throw new IllegalArgumentException(
                                    "Delegation Secretary technical meeting tasks must use title only. Remove task description and deadline.");
                        }
                    }

                    if (committeeSecretaryFlow && subcommitteeMeeting) {
                        if (taskDescription.isBlank()) {
                            throw new IllegalArgumentException(
                                    "Committee Secretary subcommittee tasks require a task description.");
                        }
                        if (taskDeadline == null) {
                            throw new IllegalArgumentException(
                                    "Committee Secretary subcommittee tasks require a mandatory timeline/deadline.");
                        }
                    }

                    // Create SubTask record for each task
                    SubTask subTask = new SubTask();
                    subTask.setTitle(taskTitle.trim());
                    subTask.setDescription(taskDescription.isBlank() ? null : taskDescription);
                    subTask.setDeadline(taskDeadline);
                    subTask.setResolution(savedResolution);
                    subTask.setMeeting(meeting);
                    subTask.setStatus(SubTask.TaskStatus.TODO);
                    subTask.setCreatedAt(LocalDateTime.now());

                    String assigneeType = (String) taskData.get("assigneeType");
                    Long subcommitteeId = null;

                    if (taskData.get("subcommitteeId") != null) {
                        subcommitteeId = Long.valueOf(taskData.get("subcommitteeId").toString());
                    }

                    if ("SUBCOMMITTEE".equals(assigneeType) && subcommitteeId != null) {
                        subTask.setSubcommitteeId(subcommitteeId);
                        assignedSubcommitteeIds.add(subcommitteeId);
                    } else if (meeting.getSubCommittee() != null) {
                        subTask.setSubcommitteeId(meeting.getSubCommittee().getId());
                        assignedSubcommitteeIds.add(meeting.getSubCommittee().getId());
                    } else {
                        // Skip task without a valid subcommittee - cannot save without subcommittee_id
                        log.warn("Skipping task '{}' - no subcommittee ID available", taskTitle);
                        continue;
                    }

                    subTaskRepository.save(subTask);
                    createdAnyTask = true;
                }

                if (committeeSecretaryFlow && subcommitteeMeeting && !createdAnyTask) {
                    throw new IllegalArgumentException(
                            "Committee Secretary subcommittee meetings must include at least one task with title, description, and deadline.");
                }

                // Create ResolutionAssignment records for each unique subcommittee
                if (!assignedSubcommitteeIds.isEmpty()) {
                    int percentagePerSubcommittee = 100 / assignedSubcommitteeIds.size();
                    int remainder = 100 % assignedSubcommitteeIds.size();
                    int index = 0;

                    for (Long subId : assignedSubcommitteeIds) {
                        try {
                            SubCommittee subcommittee = subCommitteeRepo.findById(subId).orElse(null);
                            if (subcommittee == null) {
                                log.warn("Subcommittee not found with ID: {}", subId);
                                continue;
                            }

                            ResolutionAssignment assignment = new ResolutionAssignment();
                            assignment.setResolution(savedResolution);
                            assignment.setSubcommittee(subcommittee);
                            // Distribute percentage evenly, give remainder to last subcommittee
                            int pct = percentagePerSubcommittee + (index == assignedSubcommitteeIds.size() - 1 ? remainder : 0);
                            assignment.setContributionPercentage(pct);
                            assignment.setAssignedBy(meeting.getCreatedBy() != null ? meeting.getCreatedBy().getId() : 0L);
                            assignment.setStatus(ResolutionAssignment.AssignmentStatus.ASSIGNED);
                            assignment.setAssignedAt(LocalDateTime.now());

                            resolutionAssignmentRepo.save(assignment);
                            log.info("Created resolution assignment: resolution={}, subcommittee={}, percentage={}",
                                    savedResolution.getId(), subcommittee.getName(), pct);

                            // Notify subcommittee members
                            List<User> subcommitteeUsers = userRepo.findBySubcommitteeId(subId);
                            for (User user : subcommitteeUsers) {
                                notificationService.createNotification(
                                    user.getId(),
                                    "New Task Assignment",
                                    "A new resolution has been assigned to your subcommittee: " + savedResolution.getTitle() +
                                    " (Contribution: " + pct + "%)",
                                    Notification.NotificationType.TASK_ASSIGNMENT,
                                    "Resolution",
                                    savedResolution.getId()
                                );
                            }
                        } catch (Exception e) {
                            log.error("Failed to create assignment for subcommittee {}: {}", subId, e.getMessage());
                        }
                        index++;
                    }

                    // Update resolution status to IN_PROGRESS since it now has assignments
                    savedResolution.setStatus(Resolution.ResolutionStatus.IN_PROGRESS);
                    resolutionRepo.save(savedResolution);
                }
            }
        }

        // Send notifications about new resolutions
        sendResolutionNotifications(meeting);
    }

    /**
     * Send notifications about new resolutions
     */
    private void sendResolutionNotifications(Meeting meeting) {
        // Get relevant users to notify about new resolutions
        List<User> usersToNotify = getUsersForMeetingAudience(meeting);

        for (User user : usersToNotify) {
            notificationService.createNotification(
                    user.getId(),
                    "New Resolutions Available",
                    "New resolutions have been created for meeting: " + meeting.getTitle(),
                    Notification.NotificationType.TASK_ASSIGNMENT,
                    "Resolution",
                    meeting.getId());
        }
    }

    private LocalDateTime parseDeadlineValue(Object rawDeadline) {
        if (rawDeadline == null) {
            return null;
        }

        String deadlineText = rawDeadline.toString().trim();
        if (deadlineText.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(deadlineText);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(deadlineText).atTime(23, 59, 59);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid deadline format: " + deadlineText);
            }
        }
    }

    // Utility Methods

    /**
     * Get meeting statistics for dashboard
     */
    public Map<String, Long> getMeetingStatistics(Long countryId) {
        List<Meeting> meetings = getMeetingsByCountry(countryId);

        long totalMeetings = meetings.size();
        long completedMeetings = meetings.stream()
                .filter(m -> m.getStatus() == Meeting.MeetingStatus.COMPLETED)
                .count();
        long upcomingMeetings = meetings.stream()
                .filter(m -> m.getStatus() == Meeting.MeetingStatus.SCHEDULED &&
                        m.getMeetingDate().isAfter(LocalDateTime.now()))
                .count();

        return Map.of(
                "total", totalMeetings,
                "completed", completedMeetings,
                "upcoming", upcomingMeetings);
    }

    /**
     * Get meetings for a specific user (where they are invited)
     */
    public List<Meeting> getMeetingsForUser(Long userId) {
        List<MeetingInvitation> invitations = meetingInvitationRepo.findByUserId(userId);
        return invitations.stream()
                .map(MeetingInvitation::getMeeting)
                .collect(Collectors.toList());
    }

    /**
     * Get potential invitees for a meeting based on meeting type and location
     */
    public List<Map<String, Object>> getPotentialInvitees(Long meetingId) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        // Get users based on meeting type and hosting country
        List<User> potentialInvitees = getUsersForMeetingAudience(meeting);

        // Get existing invitations to exclude already invited users
        List<MeetingInvitation> existingInvitations = meetingInvitationRepo.findByMeetingId(meetingId);
        Set<Long> invitedUserIds = existingInvitations.stream()
                .map(invitation -> invitation.getUser().getId())
                .collect(Collectors.toSet());

        // Filter out already invited users and convert to response format
        return potentialInvitees.stream()
                .filter(user -> !invitedUserIds.contains(user.getId()))
                .map(user -> {
                    Map<String, Object> invitee = new HashMap<>();
                    invitee.put("id", user.getId());
                    invitee.put("name", user.getName());
                    invitee.put("email", user.getEmail());
                    invitee.put("role", user.getRole().toString());

                    // Add country information if available
                    if (user.getCountry() != null) {
                        Map<String, Object> country = new HashMap<>();
                        country.put("id", user.getCountry().getId());
                        country.put("name", user.getCountry().getName());
                        invitee.put("country", country);
                    }

                    // Add subcommittee information if available
                    if (user.getSubcommittee() != null) {
                        Map<String, Object> subcommittee = new HashMap<>();
                        subcommittee.put("id", user.getSubcommittee().getId());
                        subcommittee.put("name", user.getSubcommittee().getName());
                        invitee.put("subcommittee", subcommittee);
                    }

                    return invitee;
                })
                .collect(Collectors.toList());
    }

    /**
     * Update meeting minutes with secretary location validation
     */
    public Meeting updateMeetingMinutesWithValidation(Long meetingId, String minutes, Long secretaryId) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        // Validate secretary can take minutes for this meeting
        SecretaryValidationService.ValidationResult validation = secretaryValidationService
                .validateMinuteTaking(secretaryId, meetingId);

        if (!validation.isValid()) {
            throw new IllegalArgumentException("Cannot update minutes: " + validation.getMessage());
        }

        // Additional location validation if secretary is from different country
        User secretary = userRepo.findById(secretaryId)
                .orElseThrow(() -> new RuntimeException("Secretary not found"));

        if (!secretaryValidationService.validateSecretaryLocation(secretary, meeting)) {
            String message = secretaryValidationService.getLocationValidationMessage(secretary, meeting);
            throw new IllegalArgumentException("Location validation failed: " + message);
        }

        meeting.setMinutes(minutes);
        meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
        meeting.setUpdatedAt(LocalDateTime.now());

        // Ensure we only update minutes and status
        return meetingRepo.save(meeting);
    }

    /**
     * NEW WORKFLOW: Add AOB (Any Other Business) items to Technical Committee meeting
     * Delegation Secretary can save multiple AOB items during TC minutes
     */
    @Transactional
    public Meeting addAOBItems(Long meetingId, List<String> aobItems, Long secretaryId) {
        log.info("Adding AOB items to meeting: meetingId={}, itemCount={}, secretaryId={}", 
                meetingId, aobItems != null ? aobItems.size() : 0, secretaryId);

        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with ID: " + meetingId));

        // Validate secretary
        User secretary = userRepo.findById(secretaryId)
                .orElseThrow(() -> new RuntimeException("Secretary not found with ID: " + secretaryId));

        if (!secretaryValidationService.isSecretary(secretary)) {
            throw new IllegalArgumentException("Only secretaries can add AOB items");
        }

        // Validate meeting type - AOB items only for Technical Committee meetings
        if (meeting.getMeetingType() != Meeting.MeetingType.TECHNICAL_MEETING) {
            throw new IllegalArgumentException("AOB items can only be added to Technical Committee meetings");
        }

        // Validate secretary has access to this meeting
        if (!secretaryValidationService.validateMeetingAccess(secretary, meeting)) {
            String message = secretaryValidationService.getLocationValidationMessage(secretary, meeting);
            throw new IllegalArgumentException("Access denied: " + message);
        }

        // Convert AOB items to JSON string
        if (aobItems != null && !aobItems.isEmpty()) {
            try {
                // Simple JSON array format: ["item1", "item2", "item3"]
                String aobJson = "[" + aobItems.stream()
                        .map(item -> "\"" + item.replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(",")) + "]";
                meeting.setAobItems(aobJson);
                log.info("AOB items saved: {}", aobJson);
            } catch (Exception e) {
                log.error("Error converting AOB items to JSON", e);
                throw new RuntimeException("Failed to save AOB items");
            }
        } else {
            meeting.setAobItems(null);
        }

        meeting.setUpdatedAt(LocalDateTime.now());
        return meetingRepo.save(meeting);
    }

    /**
     * NEW WORKFLOW: Get AOB items from meeting
     */
    public List<String> getAOBItems(Long meetingId) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with ID: " + meetingId));

        if (meeting.getAobItems() == null || meeting.getAobItems().trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Parse simple JSON array format: ["item1", "item2", "item3"]
            String aobJson = meeting.getAobItems().trim();
            if (aobJson.startsWith("[") && aobJson.endsWith("]")) {
                String content = aobJson.substring(1, aobJson.length() - 1);
                if (content.trim().isEmpty()) {
                    return new ArrayList<>();
                }
                
                List<String> items = new ArrayList<>();
                String[] parts = content.split("\",\"");
                for (String part : parts) {
                    String cleaned = part.replace("\"", "").replace("\\\"", "\"").trim();
                    if (!cleaned.isEmpty()) {
                        items.add(cleaned);
                    }
                }
                return items;
            }
        } catch (Exception e) {
            log.error("Error parsing AOB items from JSON", e);
        }

        return new ArrayList<>();
    }

    /**
     * Get meetings that a secretary can manage based on location.
     * Also auto-syncs SCHEDULED meetings that have passed their date to
     * PENDING_MINUTES.
     */
    @Transactional
    public List<Meeting> getMeetingsForSecretary(Long secretaryId) {
        User secretary = userRepo.findById(secretaryId)
                .orElseThrow(() -> new RuntimeException("Secretary not found with ID: " + secretaryId));

        if (!secretaryValidationService.isSecretary(secretary)) {
            throw new IllegalArgumentException("User is not a secretary");
        }

        Set<Meeting> meetings = new java.util.HashSet<>();

        meetings.addAll(meetingRepo.findAll());
        meetings.addAll(getMeetingsByCreator(secretaryId));

        if (meetings.isEmpty()) {
            // Return empty list instead of global findAll to prevent data leaks or
            // performance issues
            log.info("No meetings found for secretary {} within their scope constraints.", secretary.getEmail());
        }

        List<Meeting> result = meetings.stream()
            .filter(m -> secretaryValidationService.validateMeetingAccess(secretary, m))
                .filter(m -> m.getStatus() != Meeting.MeetingStatus.CANCELLED)
                .collect(Collectors.toList());

        // Auto-sync statuses based on calendar time
        syncMeetingStatuses(result);

        return result.stream()
                .sorted((m1, m2) -> {
                    if (m1.getMeetingDate() == null && m2.getMeetingDate() == null)
                        return 0;
                    if (m1.getMeetingDate() == null)
                        return 1;
                    if (m2.getMeetingDate() == null)
                        return -1;
                    return m2.getMeetingDate().compareTo(m1.getMeetingDate());
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns meetings categorized into three buckets:
     * - upcoming: future date, not yet held
     * - pendingMinutes: past date, no minutes recorded yet
     * - archived: minutes already recorded (COMPLETED)
     */
    @Transactional
    public Map<String, List<Meeting>> getMeetingsForSecretaryByCategory(Long secretaryId) {
        List<Meeting> all = getMeetingsForSecretary(secretaryId);
        LocalDateTime now = LocalDateTime.now();

        List<Meeting> upcoming = all.stream()
            .filter(m -> m.getMeetingDate() != null && m.getMeetingDate().isAfter(now))
                .collect(Collectors.toList());

        List<Meeting> ongoing = all.stream()
            .filter(m -> m.getMeetingDate() != null
                && !m.getMeetingDate().isAfter(now)
                && getEffectiveMeetingEndDate(m).isAfter(now)
                && m.getStatus() != Meeting.MeetingStatus.COMPLETED
                && m.getStatus() != Meeting.MeetingStatus.CANCELLED)
            .collect(Collectors.toList());

        List<Meeting> pendingMinutes = all.stream()
            .filter(m -> m.getMeetingDate() != null
                && !getEffectiveMeetingEndDate(m).isAfter(now)
                        && m.getStatus() != Meeting.MeetingStatus.COMPLETED
                        && m.getStatus() != Meeting.MeetingStatus.CANCELLED
                && (m.getMinutes() == null || m.getMinutes().isBlank())
                && (m.getMinutesDocument() == null || m.getMinutesDocument().isBlank()))
                .collect(Collectors.toList());

        List<Meeting> archived = all.stream()
                .filter(m -> (m.getStatus() != null && m.getStatus() == Meeting.MeetingStatus.COMPLETED)
                || (m.getMinutes() != null && !m.getMinutes().isBlank())
                || (m.getMinutesDocument() != null && !m.getMinutesDocument().isBlank()))
                .collect(Collectors.toList());

        return Map.of(
                "upcoming", upcoming,
            "ongoing", ongoing,
                "pendingMinutes", pendingMinutes,
                "archived", archived);
    }

    /**
     * Auto-transitions SCHEDULED meetings to PENDING_MINUTES if their
     * date has passed and they have no minutes recorded yet.
     * Public so controllers/endpoints can trigger sync for any meeting list.
     */
    @Transactional
    public void syncMeetingStatuses(List<Meeting> meetings) {
        LocalDateTime now = LocalDateTime.now();
        for (Meeting m : meetings) {
            if (m.getMeetingDate() == null)
                continue;
            LocalDateTime effectiveEndDate = getEffectiveMeetingEndDate(m);
            if (m.getStatus() != null && (m.getStatus() == Meeting.MeetingStatus.SCHEDULED
                    || m.getStatus() == Meeting.MeetingStatus.IN_PROGRESS)) {
                if (!m.getMeetingDate().isAfter(now) && effectiveEndDate.isAfter(now)) {
                    m.setStatus(Meeting.MeetingStatus.IN_PROGRESS);
                    m.setUpdatedAt(now);
                    meetingRepo.save(m);
                    continue;
                }
                if (!effectiveEndDate.isAfter(now)) {
                    if ((m.getMinutes() != null && !m.getMinutes().isBlank())
                            || (m.getMinutesDocument() != null && !m.getMinutesDocument().isBlank())) {
                        m.setStatus(Meeting.MeetingStatus.COMPLETED);
                    } else {
                        m.setStatus(Meeting.MeetingStatus.PENDING_MINUTES);
                    }
                    m.setUpdatedAt(now);
                    meetingRepo.save(m);
                }
            }
        }
    }

    /**
     * Validate secretary can perform meeting operations
     */
    public boolean validateSecretaryMeetingAccess(Long secretaryId, Long meetingId) {
        try {
            User secretary = userRepo.findById(secretaryId)
                    .orElseThrow(() -> new RuntimeException("Secretary not found"));

            Meeting meeting = meetingRepo.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Meeting not found"));

            return secretaryValidationService.validateSecretaryLocation(secretary, meeting);
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDateTime getEffectiveMeetingEndDate(Meeting meeting) {
        if (meeting.getMeetingEndDate() != null) {
            return meeting.getMeetingEndDate();
        }
        return meeting.getMeetingDate();
    }

    public Meeting uploadMinutesDocuments(Long meetingId, List<MultipartFile> minutesDocuments) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (minutesDocuments == null || minutesDocuments.isEmpty()) {
            throw new IllegalArgumentException("At least one minutes document is required");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }

        List<String> existingDocuments = parseMinutesDocuments(meeting.getMinutesDocument());

        for (MultipartFile minutesDocument : minutesDocuments) {
            if (minutesDocument == null || minutesDocument.isEmpty()) {
                continue;
            }

            validateMinutesDocument(minutesDocument);

            String storedFileName = generateStoredMinutesFilename(minutesDocument.getOriginalFilename());
            Path targetLocation = uploadPath.resolve(storedFileName);

            try {
                Files.copy(minutesDocument.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to store minutes document: " + minutesDocument.getOriginalFilename(), ex);
            }

            existingDocuments.add(storedFileName);
        }

        if (existingDocuments.isEmpty()) {
            throw new IllegalArgumentException("No valid minutes documents were provided");
        }

        meeting.setMinutesDocument(String.join(",", existingDocuments));
        meeting.setUpdatedAt(LocalDateTime.now());

        if (meeting.getStatus() == Meeting.MeetingStatus.PENDING_MINUTES || meeting.getStatus() == Meeting.MeetingStatus.IN_PROGRESS) {
            meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
        }

        return meetingRepo.save(meeting);
    }

    private List<String> parseMinutesDocuments(String rawValue) {
        List<String> files = new ArrayList<>();
        if (rawValue == null || rawValue.isBlank()) {
            return files;
        }

        String[] tokens = rawValue.split(",");
        for (String token : tokens) {
            String candidate = token == null ? "" : token.trim();
            if (!candidate.isEmpty()) {
                files.add(candidate);
            }
        }
        return files;
    }

    private String generateStoredMinutesFilename(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "minutes" : originalFilename);
        String extension = "";
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < cleaned.length() - 1) {
            extension = cleaned.substring(dotIndex).toLowerCase();
        }
        return "minutes_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
    }

    private void validateMinutesDocument(MultipartFile file) {
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Minutes document exceeds maximum allowed size of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Minutes document content type is invalid");
        }

        boolean allowed = contentType.equals("application/pdf")
                || contentType.equals("application/msword")
                || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || contentType.equals("text/plain")
                || contentType.startsWith("image/");

        if (!allowed) {
            throw new IllegalArgumentException("Minutes document type is not allowed: " + contentType);
        }
    }

    public Meeting createArchivedRecord(Meeting meeting, User creator) {
        if (meeting.getTitle() == null || meeting.getTitle().isBlank()) {
            throw new IllegalArgumentException("Meeting title is required");
        }
        if (meeting.getMeetingDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (meeting.getHostingCountry() == null || meeting.getHostingCountry().getId() == null) {
            throw new IllegalArgumentException("Hosting country is required");
        }
        if (creator == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        User fullCreator = userRepo.findById(creator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        Country hostingCountry = countryRepo.findById(meeting.getHostingCountry().getId())
                .orElseThrow(() -> new IllegalArgumentException("Hosting country not found"));

        meeting.setCreatedBy(fullCreator);
        meeting.setHostingCountry(hostingCountry);
        meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
        if (meeting.getMeetingEndDate() == null) {
            meeting.setMeetingEndDate(meeting.getMeetingDate());
        }
        if (meeting.getMeetingMode() == null) {
            meeting.setMeetingMode(Meeting.MeetingMode.PHYSICAL);
        }

        return meetingRepo.save(meeting);
    }
}