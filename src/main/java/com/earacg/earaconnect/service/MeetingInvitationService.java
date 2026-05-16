package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Notification;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.MeetingInvitation;
import com.earacg.earaconnect.repository.MeetingInvitationRepo;
import com.earacg.earaconnect.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MeetingInvitationService {

    @Autowired
    private MeetingInvitationRepo meetingInvitationRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

    public List<MeetingInvitation> getAllInvitations() {
        return meetingInvitationRepo.findAll();
    }

    public Optional<MeetingInvitation> getInvitationById(Long id) {
        return meetingInvitationRepo.findById(id);
    }

    public List<MeetingInvitation> getInvitationsByMeeting(Long meetingId) {
        return meetingInvitationRepo.findByMeetingId(meetingId);
    }

    public List<MeetingInvitation> getInvitationsByUser(Long userId) {
        return meetingInvitationRepo.findByUserId(userId);
    }

    public List<MeetingInvitation> getInvitationsByStatus(MeetingInvitation.InvitationStatus status) {
        return meetingInvitationRepo.findByStatus(status);
    }

    public MeetingInvitation createInvitation(MeetingInvitation invitation) {
        if (invitation.getMeeting() == null || invitation.getUser() == null) {
            return null;
        }
        
        invitation.setSentAt(LocalDateTime.now());
        invitation.setStatus(MeetingInvitation.InvitationStatus.PENDING);
        
        return meetingInvitationRepo.save(invitation);
    }

    public MeetingInvitation updateInvitation(Long id, MeetingInvitation invitationDetails) {
        Optional<MeetingInvitation> invitationOpt = meetingInvitationRepo.findById(id);
        if (invitationOpt.isPresent()) {
            MeetingInvitation invitation = invitationOpt.get();
            
            if (invitationDetails.getStatus() != null) {
                invitation.setStatus(invitationDetails.getStatus());
            }
            
            if (invitationDetails.getResponseComment() != null) {
                invitation.setResponseComment(invitationDetails.getResponseComment());
            }
            
            if (invitationDetails.getStatus() != null && 
                invitationDetails.getStatus() != MeetingInvitation.InvitationStatus.PENDING) {
                invitation.setRespondedAt(LocalDateTime.now());
            }
            
            return meetingInvitationRepo.save(invitation);
        }
        return null;
    }

    public boolean deleteInvitation(Long id) {
        if (meetingInvitationRepo.existsById(id)) {
            meetingInvitationRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public MeetingInvitation respondToInvitation(Long id, MeetingInvitation.InvitationStatus status, String comment) {
        Optional<MeetingInvitation> invitationOpt = meetingInvitationRepo.findById(id);
        if (invitationOpt.isPresent()) {
            MeetingInvitation invitation = invitationOpt.get();
            
            invitation.setStatus(status);
            invitation.setResponseComment(comment);
            invitation.setRespondedAt(LocalDateTime.now());

            MeetingInvitation savedInvitation = meetingInvitationRepo.save(invitation);
            sendAttendanceResponseNotifications(savedInvitation, status);

            return savedInvitation;
        }
        return null;
    }

    private void sendAttendanceResponseNotifications(MeetingInvitation invitation,
                                                     MeetingInvitation.InvitationStatus invitationStatus) {
        if (invitation.getMeeting() == null || invitation.getUser() == null) {
            return;
        }

        String memberName = invitation.getUser().getName() != null
            ? invitation.getUser().getName()
            : invitation.getUser().getEmail();
        String meetingTitle = invitation.getMeeting().getTitle() != null
            ? invitation.getMeeting().getTitle()
            : "Untitled meeting";
        String responseLabel = invitationStatus.name().replace("_", " ");
        String details = "Meeting: " + meetingTitle + " | Member: " + memberName + " | Response: " + responseLabel;

        Set<Long> notifiedUserIds = new HashSet<>();

        if (invitation.getMeeting().getCreatedBy() != null
            && invitation.getMeeting().getCreatedBy().getId() != null) {
            Long creatorId = invitation.getMeeting().getCreatedBy().getId();
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

        if (invitation.getMeeting().getHostingCountry() != null
            && invitation.getMeeting().getHostingCountry().getId() != null) {
            List<User> hodUsers = userRepo.findByRoleAndCountryId(
                User.UserRole.HOD,
                invitation.getMeeting().getHostingCountry().getId()
            );

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
}
