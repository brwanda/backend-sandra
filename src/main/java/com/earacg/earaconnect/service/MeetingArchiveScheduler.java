package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Meeting;
import com.earacg.earaconnect.model.Notification;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.MeetingRepo;
import com.earacg.earaconnect.repository.NotificationRepo;
import com.earacg.earaconnect.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduled service that automatically transitions past meetings
 * from SCHEDULED/IN_PROGRESS to PENDING_MINUTES or COMPLETED.
 * Also alerts secretaries about late meeting minutes (3+ days overdue).
 * Runs every 5 minutes.
 */
@Service
public class MeetingArchiveScheduler {

    @Autowired
    private MeetingRepo meetingRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepo notificationRepo;

    private static final long ALERT_DEDUP_HOURS = 24;

    /**
     * Every 5 minutes, find meetings whose date has passed and are still
     * SCHEDULED or IN_PROGRESS, and transition them appropriately.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void archiveExpiredMeetings() {
        LocalDateTime now = LocalDateTime.now();

        List<Meeting> activeMeetings = meetingRepo.findByStatusIn(
                List.of(Meeting.MeetingStatus.SCHEDULED, Meeting.MeetingStatus.IN_PROGRESS));

        int transitioned = 0;
        for (Meeting meeting : activeMeetings) {
            if (meeting.getMeetingDate() != null && meeting.getMeetingDate().isBefore(now)) {
                if (meeting.getMinutes() != null && !meeting.getMinutes().isBlank()) {
                    meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
                } else {
                    meeting.setStatus(Meeting.MeetingStatus.PENDING_MINUTES);
                }
                meetingRepo.save(meeting);
                transitioned++;
            }
        }

        if (transitioned > 0) {
            System.out.println("[MeetingArchiveScheduler] Transitioned " + transitioned + " past meetings.");
        }
    }

    /**
     * Every hour, check for meetings that have been PENDING_MINUTES for more than 3 days
     * and alert the secretary who created them.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void alertLateMinutes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);

        List<Meeting> pendingMinutesMeetings = meetingRepo.findByStatus(Meeting.MeetingStatus.PENDING_MINUTES);

        int alerted = 0;
        for (Meeting meeting : pendingMinutesMeetings) {
            // Check if meeting date was more than 3 days ago and still no minutes
            if (meeting.getMeetingDate() != null && meeting.getMeetingDate().isBefore(threeDaysAgo)) {
                if (meeting.getMinutes() == null || meeting.getMinutes().isBlank()) {
                    long daysSinceSettled = ChronoUnit.DAYS.between(meeting.getMeetingDate(), now);
                    String title = "Late Meeting Minutes Alert";
                    String message = "⚠️ LATE MINUTES: Meeting '" + meeting.getTitle()
                            + "' has been pending minutes for " + daysSinceSettled
                            + " days (exceeded 3-day limit). Please record the minutes as soon as possible.";

                    Set<Long> recipientIds = new HashSet<>();

                    // Alert the creator when available.
                    if (meeting.getCreatedBy() != null && meeting.getCreatedBy().getId() != null) {
                        recipientIds.add(meeting.getCreatedBy().getId());
                    }

                    // Alert country-level secretaries.
                    if (meeting.getHostingCountry() != null && meeting.getHostingCountry().getId() != null) {
                        Long countryId = meeting.getHostingCountry().getId();
                        for (User sec : userRepo.findByRoleAndCountryId(User.UserRole.SECRETARY, countryId)) {
                            recipientIds.add(sec.getId());
                        }
                        for (User sec : userRepo.findByRoleAndCountryId(User.UserRole.COMMITTEE_SECRETARY, countryId)) {
                            recipientIds.add(sec.getId());
                        }
                        for (User sec : userRepo.findByRoleAndCountryId(User.UserRole.DELEGATION_SECRETARY, countryId)) {
                            recipientIds.add(sec.getId());
                        }
                    }

                    for (Long recipientId : recipientIds) {
                        if (recipientId == null) continue;
                        if (wasLateMinutesAlertSentRecently(recipientId, meeting.getId(), now)) continue;

                        notificationService.createNotification(
                                recipientId,
                                title,
                                message,
                                Notification.NotificationType.GENERAL_ANNOUNCEMENT,
                                "Meeting",
                                meeting.getId());
                    }
                    alerted++;
                }
            }
        }

        if (alerted > 0) {
            System.out
                    .println("[MeetingArchiveScheduler] Sent late minutes alerts for " + alerted + " meetings.");
        }
    }

    private boolean wasLateMinutesAlertSentRecently(Long userId, Long meetingId, LocalDateTime now) {
        return notificationRepo.existsByUserIdAndTypeAndRelatedEntityTypeAndRelatedEntityIdAndCreatedAtAfter(
                userId,
                Notification.NotificationType.GENERAL_ANNOUNCEMENT,
                "Meeting",
                meetingId,
                now.minusHours(ALERT_DEDUP_HOURS));
    }
}
