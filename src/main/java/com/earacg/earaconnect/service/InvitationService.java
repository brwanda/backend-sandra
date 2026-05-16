package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.*;
import com.earacg.earaconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvitationService {
    
    @Autowired
    private MeetingRepo meetingRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private MeetingInvitationRepo meetingInvitationRepo;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private CommitteeRepo committeeRepo;
    
    @Autowired
    private SubCommitteeRepo subCommitteeRepo;
    
    @Autowired
    private CountryCommitteeMemberRepo countryCommitteeMemberRepo;
    
    /**
     * Send invitations to multiple users for a meeting
     */
    public Map<String, Object> sendInvitations(Long meetingId, Long senderId, 
                                             List<Long> recipientIds, String message, Boolean sendEmail) {
        
        // Validate meeting exists
        Meeting meeting = meetingRepo.findById(meetingId)
            .orElseThrow(() -> new RuntimeException("Meeting not found"));
        
        // Validate sender exists and has permission
        User sender = userRepo.findById(senderId)
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        // Allow all secretary variants used by the UI to send invitations
        if (sender.getRole() != User.UserRole.SECRETARY
                && sender.getRole() != User.UserRole.COMMITTEE_SECRETARY
                && sender.getRole() != User.UserRole.DELEGATION_SECRETARY) {
            throw new RuntimeException("Only secretary roles can send meeting invitations");
        }
        
        // Check if secretary's country matches meeting's hosting country
        if (!meeting.getHostingCountry().getId().equals(sender.getCountry().getId())) {
            throw new RuntimeException("Secretary can only send invitations for meetings in their country");
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (Long recipientId : recipientIds) {
            try {
                User recipient = userRepo.findById(recipientId)
                    .orElseThrow(() -> new RuntimeException("Recipient not found: " + recipientId));
                
                // Check if invitation already exists
                Optional<MeetingInvitation> existingInvitation = meetingInvitationRepo
                    .findByMeetingIdAndUserId(meetingId, recipientId);
                
                MeetingInvitation invitation;
                boolean isNew = false;
                
                if (existingInvitation.isPresent()) {
                    invitation = existingInvitation.get();
                    // Update existing invitation
                    invitation.setStatus(MeetingInvitation.InvitationStatus.PENDING);
                    invitation.setSentAt(LocalDateTime.now());
                } else {
                    // Create new invitation
                    invitation = new MeetingInvitation();
                    invitation.setMeeting(meeting);
                    invitation.setUser(recipient);
                    invitation.setStatus(MeetingInvitation.InvitationStatus.PENDING);
                    invitation.setSentAt(LocalDateTime.now());
                    isNew = true;
                }
                
                // Save invitation
                meetingInvitationRepo.save(invitation);
                
                // Send email if requested
                if (sendEmail != null && sendEmail) {
                    try {
                        String emailSubject = "Meeting Invitation: " + meeting.getTitle();
                        String emailBody = message != null ? message : createDefaultInvitationMessage(meeting, recipient);
                        
                        emailService.sendMeetingInvitation(
                            recipient.getEmail(),
                            recipient.getName(),
                            meeting.getTitle(),
                            meeting.getMeetingDate().toString(),
                            meeting.getLocation()
                        );
                    } catch (Exception emailError) {
                        // Log email error but don't fail the invitation
                        System.err.println("Failed to send email to " + recipient.getEmail() + ": " + emailError.getMessage());
                    }
                }
                
                // Create in-system notification
                notificationService.createNotification(
                    recipient.getId(),
                    "Meeting Invitation",
                    "You have been invited to attend: " + meeting.getTitle(),
                    Notification.NotificationType.MEETING_INVITATION,
                    "Meeting",
                    meeting.getId()
                );
                
                // Add to results
                Map<String, Object> result = new HashMap<>();
                result.put("recipientId", recipientId);
                result.put("recipientName", recipient.getName());
                result.put("recipientEmail", recipient.getEmail());
                result.put("status", "success");
                result.put("message", isNew ? "Invitation sent" : "Invitation updated");
                results.add(result);
                
                successCount++;
                
            } catch (Exception e) {
                // Add error to results
                Map<String, Object> result = new HashMap<>();
                result.put("recipientId", recipientId);
                result.put("status", "error");
                result.put("message", "Failed to send invitation: " + e.getMessage());
                results.add(result);
                
                failureCount++;
            }
        }
        
        // Return summary
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invitations processed");
        response.put("totalSent", recipientIds.size());
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        response.put("results", results);
        response.put("meetingId", meetingId);
        response.put("meetingTitle", meeting.getTitle());
        
        return response;
    }
    
    /**
     * Get all invitations for a meeting
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMeetingInvitations(Long meetingId) {
        List<MeetingInvitation> invitations = meetingInvitationRepo.findByMeetingId(meetingId);
        
        return invitations.stream().map(invitation -> {
            Map<String, Object> invitationMap = new HashMap<>();
            invitationMap.put("id", invitation.getId());
            invitationMap.put("status", invitation.getStatus().toString());
            invitationMap.put("sentAt", invitation.getSentAt());
            invitationMap.put("respondedAt", invitation.getRespondedAt());
            invitationMap.put("responseComment", invitation.getResponseComment());
            
            // Add user info
            User user = invitation.getUser();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole().toString());
            if (user.getCountry() != null) {
                userInfo.put("country", user.getCountry().getName());
            }
            invitationMap.put("user", userInfo);
            
            return invitationMap;
        }).collect(Collectors.toList());
    }
    
    /**
     * Get all invitations for a user
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserInvitations(Long userId) {
        List<MeetingInvitation> invitations = meetingInvitationRepo.findByUserId(userId);
        
        return invitations.stream().map(invitation -> {
            Map<String, Object> invitationMap = new HashMap<>();
            invitationMap.put("id", invitation.getId());
            invitationMap.put("status", invitation.getStatus().toString());
            invitationMap.put("sentAt", invitation.getSentAt());
            invitationMap.put("respondedAt", invitation.getRespondedAt());
            invitationMap.put("responseComment", invitation.getResponseComment());
            
            // Add meeting info
            Meeting meeting = invitation.getMeeting();
            Map<String, Object> meetingInfo = new HashMap<>();
            meetingInfo.put("id", meeting.getId());
            meetingInfo.put("title", meeting.getTitle());
            meetingInfo.put("description", meeting.getDescription());
            meetingInfo.put("meetingDate", meeting.getMeetingDate());
            meetingInfo.put("location", meeting.getLocation());
            meetingInfo.put("status", meeting.getStatus().toString());
            meetingInfo.put("type", meeting.getMeetingType().toString());
            invitationMap.put("meeting", meetingInfo);
            
            return invitationMap;
        }).collect(Collectors.toList());
    }
    
    /**
     * Respond to an invitation
     */
    public Map<String, Object> respondToInvitation(Long invitationId, String status, String comment, Long currentUserId) {
        MeetingInvitation invitation = meetingInvitationRepo.findById(invitationId)
            .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (currentUserId == null || invitation.getUser() == null || invitation.getUser().getId() == null) {
            throw new IllegalArgumentException("Unable to validate invitation ownership.");
        }
        if (!invitation.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You can only respond to your own meeting invitations.");
        }
        
        // Update invitation
        MeetingInvitation.InvitationStatus invitationStatus;
        try {
            invitationStatus = MeetingInvitation.InvitationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status. Must be: PENDING, ACCEPTED, DECLINED, or MAYBE");
        }
        
        invitation.setStatus(invitationStatus);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setResponseComment(comment);
        
        meetingInvitationRepo.save(invitation);

        // Notify meeting creator and HOD(s) with clear attendance response context.
        sendAttendanceResponseNotifications(invitation, invitationStatus);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Response recorded successfully");
        response.put("invitationId", invitationId);
        response.put("status", status);
        response.put("meetingTitle", invitation.getMeeting() != null ? invitation.getMeeting().getTitle() : null);
        
        return response;
    }

    private void sendAttendanceResponseNotifications(MeetingInvitation invitation,
                                                     MeetingInvitation.InvitationStatus invitationStatus) {
        Meeting meeting = invitation.getMeeting();
        User respondent = invitation.getUser();

        if (meeting == null || respondent == null) {
            return;
        }

        String memberName = respondent.getName() != null ? respondent.getName() : respondent.getEmail();
        String meetingTitle = meeting.getTitle() != null ? meeting.getTitle() : "Untitled meeting";
        String responseLabel = invitationStatus.name().replace("_", " ");
        String details = "Meeting: " + meetingTitle + " | Member: " + memberName + " | Response: " + responseLabel;

        Set<Long> notifiedUserIds = new HashSet<>();

        if (meeting.getCreatedBy() != null && meeting.getCreatedBy().getId() != null) {
            Long creatorId = meeting.getCreatedBy().getId();
            notificationService.createNotification(
                creatorId,
                "Meeting Attendance Response",
                details,
                Notification.NotificationType.MEETING_INVITATION,
                "MeetingInvitation",
                invitation.getId()
            );
            notifiedUserIds.add(creatorId);
        }

        if (meeting.getHostingCountry() != null && meeting.getHostingCountry().getId() != null) {
            List<User> hodUsers = userRepo.findByRoleAndCountryId(User.UserRole.HOD, meeting.getHostingCountry().getId());
            for (User hod : hodUsers) {
                if (hod == null || hod.getId() == null || notifiedUserIds.contains(hod.getId())) {
                    continue;
                }
                notificationService.createNotification(
                    hod.getId(),
                    "Meeting Attendance Response",
                    details,
                    Notification.NotificationType.MEETING_INVITATION,
                    "MeetingInvitation",
                    invitation.getId()
                );
            }
        }
    }
    
    /**
     * Create default invitation message
     */
    private String createDefaultInvitationMessage(Meeting meeting, User recipient) {
        return String.format(
            "Dear %s,\n\n" +
            "You are cordially invited to attend the following meeting:\n\n" +
            "Title: %s\n" +
            "Date: %s\n" +
            "Location: %s\n\n" +
            "%s\n\n" +
            "Please confirm your attendance in EaraConnect (Member Dashboard → Meetings tab).\n\n" +
            "Best regards,\n" +
            "EARA Secretariat",
            recipient.getName(),
            meeting.getTitle(),
            meeting.getMeetingDate(),
            meeting.getLocation(),
            meeting.getDescription() != null ? meeting.getDescription() : ""
        );
    }
    
    /**
     * Send bulk invitations to committee and subcommittee members
     */
    public Map<String, Object> sendBulkInvitations(Long meetingId, List<Long> committeeIds, 
                                                  List<Long> subcommitteeIds, String message, Boolean sendEmail) {
        
        // Validate meeting exists
        Meeting meeting = meetingRepo.findById(meetingId)
            .orElseThrow(() -> new RuntimeException("Meeting not found"));
        
        Set<User> allRecipients = new HashSet<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        int successCount = 0;
        int errorCount = 0;
        int emailSuccessCount = 0;
        int emailFailureCount = 0;
        
        try {
            // Get users from committees
            for (Long committeeId : committeeIds) {
                try {
                    Committee committee = committeeRepo.findById(committeeId)
                        .orElseThrow(() -> new RuntimeException("Committee not found: " + committeeId));
                    
                    // Get committee members and find corresponding users
                    List<CountryCommitteeMember> committeeMembers = countryCommitteeMemberRepo.findByCommitteeId(committeeId);
                    List<User> committeeUsers = new ArrayList<>();
                    
                    for (CountryCommitteeMember member : committeeMembers) {
                        // Try to find user by email
                        Optional<User> userOpt = userRepo.findByEmail(member.getEmail());
                        if (userOpt.isPresent()) {
                            committeeUsers.add(userOpt.get());
                        }
                        // Note: If user doesn't exist in User table, we skip them for now
                        // In a production system, you might want to create users or handle this differently
                    }
                    
                    allRecipients.addAll(committeeUsers);
                    
                    // Track email delivery for this committee
                    int committeeEmailSuccess = 0;
                    int committeeEmailFailure = 0;
                    
                    if (sendEmail != null && sendEmail) {
                        for (User user : committeeUsers) {
                            try {
                                System.out.println("📧 Attempting to send email to: " + user.getEmail());
                                emailService.sendMeetingInvitation(
                                    user.getEmail(),
                                    user.getName(),
                                    meeting.getTitle(),
                                    meeting.getMeetingDate().toString(),
                                    meeting.getLocation()
                                );
                                committeeEmailSuccess++;
                                emailSuccessCount++;
                                System.out.println("✅ Email sent successfully to: " + user.getEmail());
                            } catch (Exception emailError) {
                                committeeEmailFailure++;
                                emailFailureCount++;
                                System.err.println("❌ Failed to send email to committee member " + user.getEmail() + ": " + emailError.getMessage());
                                emailError.printStackTrace();
                            }
                        }
                    }
                    
                    results.add(Map.of(
                        "type", "committee",
                        "id", committeeId,
                        "name", committee.getName(),
                        "memberCount", committeeUsers.size(),
                        "status", "success",
                        "emailSuccessCount", committeeEmailSuccess,
                        "emailFailureCount", committeeEmailFailure,
                        "emailStatus", committeeEmailSuccess > 0 ? "emails_sent" : "no_emails"
                    ));
                } catch (Exception e) {
                    results.add(Map.of(
                        "type", "committee",
                        "id", committeeId,
                        "status", "error",
                        "error", e.getMessage()
                    ));
                    errorCount++;
                }
            }
            
            // Get users from subcommittees
            for (Long subcommitteeId : subcommitteeIds) {
                try {
                    SubCommittee subcommittee = subCommitteeRepo.findById(subcommitteeId)
                        .orElseThrow(() -> new RuntimeException("Subcommittee not found: " + subcommitteeId));
                    
                    List<User> subcommitteeUsers = userRepo.findBySubcommitteeId(subcommitteeId);
                    allRecipients.addAll(subcommitteeUsers);
                    
                    // Track email delivery for this subcommittee
                    int subcommitteeEmailSuccess = 0;
                    int subcommitteeEmailFailure = 0;
                    
                    if (sendEmail != null && sendEmail) {
                        for (User user : subcommitteeUsers) {
                            try {
                                System.out.println("📧 Attempting to send email to subcommittee member: " + user.getEmail());
                                emailService.sendMeetingInvitation(
                                    user.getEmail(),
                                    user.getName(),
                                    meeting.getTitle(),
                                    meeting.getMeetingDate().toString(),
                                    meeting.getLocation()
                                );
                                subcommitteeEmailSuccess++;
                                emailSuccessCount++;
                                System.out.println("✅ Email sent successfully to subcommittee member: " + user.getEmail());
                            } catch (Exception emailError) {
                                subcommitteeEmailFailure++;
                                emailFailureCount++;
                                System.err.println("❌ Failed to send email to subcommittee member " + user.getEmail() + ": " + emailError.getMessage());
                                emailError.printStackTrace();
                            }
                        }
                    }
                    
                    results.add(Map.of(
                        "type", "subcommittee",
                        "id", subcommitteeId,
                        "name", subcommittee.getName(),
                        "memberCount", subcommitteeUsers.size(),
                        "status", "success",
                        "emailSuccessCount", subcommitteeEmailSuccess,
                        "emailFailureCount", subcommitteeEmailFailure,
                        "emailStatus", subcommitteeEmailSuccess > 0 ? "emails_sent" : "no_emails"
                    ));
                } catch (Exception e) {
                    results.add(Map.of(
                        "type", "subcommittee",
                        "id", subcommitteeId,
                        "status", "error",
                        "error", e.getMessage()
                    ));
                    errorCount++;
                }
            }
            
            // Send invitations to all unique recipients
            for (User recipient : allRecipients) {
                try {
                    // Check if invitation already exists
                    Optional<MeetingInvitation> existingInvitation = 
                        meetingInvitationRepo.findByMeetingIdAndUserId(meetingId, recipient.getId());
                    
                    MeetingInvitation invitation;
                    boolean isNew = false;
                    
                    if (existingInvitation.isPresent()) {
                        invitation = existingInvitation.get();
                        invitation.setStatus(MeetingInvitation.InvitationStatus.PENDING);
                        invitation.setSentAt(LocalDateTime.now());
                    } else {
                        invitation = new MeetingInvitation();
                        invitation.setMeeting(meeting);
                        invitation.setUser(recipient);
                        invitation.setStatus(MeetingInvitation.InvitationStatus.PENDING);
                        invitation.setSentAt(LocalDateTime.now());
                        isNew = true;
                    }
                    
                    meetingInvitationRepo.save(invitation);
                    
                    // Create in-system notification
                    notificationService.createNotification(
                        recipient.getId(),
                        "Meeting Invitation",
                        "You have been invited to attend: " + meeting.getTitle(),
                        Notification.NotificationType.MEETING_INVITATION,
                        "Meeting",
                        meeting.getId()
                    );
                    
                    successCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("Failed to send invitation to user " + recipient.getId() + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", String.format("Bulk invitations processed. Success: %d, Errors: %d", successCount, errorCount));
            response.put("totalRecipients", allRecipients.size());
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("emailDeliveryStats", Map.of(
                "totalEmailsAttempted", emailSuccessCount + emailFailureCount,
                "emailsSentSuccessfully", emailSuccessCount,
                "emailsFailed", emailFailureCount,
                "emailSuccessRate", allRecipients.size() > 0 ? 
                    String.format("%.1f%%", (double) emailSuccessCount / allRecipients.size() * 100) : "0%"
            ));
            response.put("results", results);
            response.put("meetingId", meetingId);
            response.put("meetingTitle", meeting.getTitle());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to process bulk invitations: " + e.getMessage());
            response.put("results", results);
        }
        
        return response;
    }
}