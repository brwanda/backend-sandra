package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Meeting;
import com.earacg.earaconnect.model.Notification;
import com.earacg.earaconnect.repository.MeetingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled service that monitors meeting deadlines and sends notifications.
 * 
 * Business rules:
 * - Meeting minutes must be submitted within 3 days after the meeting ends.
 * - A reminder is sent at the 2-day mark if minutes are still missing.
 * - An overdue notification is sent when the 3-day deadline has passed.
 * - Notifications are sent to the meeting creator (the Secretary who created it).
 */
@Service
public class DeadlineNotificationService {

    /** Minutes deadline: 3 calendar days after the meeting date */
    private static final int MINUTES_DEADLINE_DAYS = 3;

    /** Reminder fires 1 day before the deadline (i.e. 2 days after the meeting) */
    private static final int REMINDER_DAYS = MINUTES_DEADLINE_DAYS - 1;

    @Autowired
    private MeetingRepo meetingRepo;

    @Autowired
    private NotificationService notificationService;

    /**
     * Runs every 6 hours to check for approaching and overdue minutes deadlines.
     * 
     * Checks all meetings in PENDING_MINUTES status:
     *  - If 2+ days have passed since meetingDate → send reminder (if not already sent recently)
     *  - If 3+ days have passed → send overdue notification and potentially auto-complete meeting
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // every 6 hours
    public void checkMinutesDeadlines() {
        List<Meeting> pendingMeetings = meetingRepo.findByStatus(Meeting.MeetingStatus.PENDING_MINUTES);

        LocalDateTime now = LocalDateTime.now();

        for (Meeting meeting : pendingMeetings) {
            if (meeting.getMeetingDate() == null || meeting.getCreatedBy() == null) {
                continue;
            }

            long daysSinceMeeting = ChronoUnit.DAYS.between(meeting.getMeetingDate(), now);
            Long creatorId = meeting.getCreatedBy().getId();

            // Check for overdue (3+ days and still no minutes)
            if (daysSinceMeeting >= MINUTES_DEADLINE_DAYS && isMinutesMissing(meeting)) {
                notificationService.createNotification(
                    creatorId,
                    "⚠️ Minutes Deadline Overdue",
                    String.format(
                        "The meeting \"%s\" (held on %s) is %d day(s) past the %d-day minutes submission deadline. " +
                        "Please submit meeting minutes immediately.",
                        meeting.getTitle(),
                        meeting.getMeetingDate().toLocalDate(),
                        daysSinceMeeting,
                        MINUTES_DEADLINE_DAYS
                    ),
                    Notification.NotificationType.DEADLINE_OVERDUE,
                    "MEETING",
                    meeting.getId()
                );
            }
            // Check for approaching deadline (2 days)
            else if (daysSinceMeeting >= REMINDER_DAYS && isMinutesMissing(meeting)) {
                notificationService.createNotification(
                    creatorId,
                    "⏰ Minutes Deadline Approaching",
                    String.format(
                        "Reminder: The meeting \"%s\" (held on %s) has a %d-day deadline for minutes submission. " +
                        "You have %d day(s) remaining.",
                        meeting.getTitle(),
                        meeting.getMeetingDate().toLocalDate(),
                        MINUTES_DEADLINE_DAYS,
                        MINUTES_DEADLINE_DAYS - daysSinceMeeting
                    ),
                    Notification.NotificationType.DEADLINE_REMINDER,
                    "MEETING",
                    meeting.getId()
                );
            }
        }
    }

    /**
     * Also check for meetings stuck in IN_PROGRESS beyond 24 hours — likely
     * the secretary forgot to change status.  Send a gentle reminder.
     */
    @Scheduled(fixedRate = 12 * 60 * 60 * 1000) // every 12 hours
    public void checkStaleInProgressMeetings() {
        List<Meeting> inProgressMeetings = meetingRepo.findByStatus(Meeting.MeetingStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();

        for (Meeting meeting : inProgressMeetings) {
            if (meeting.getMeetingDate() == null || meeting.getCreatedBy() == null) continue;

            long hoursSinceStart = ChronoUnit.HOURS.between(meeting.getMeetingDate(), now);

            // If a meeting has been "in progress" for more than 24 hours, it's probably over
            if (hoursSinceStart > 24) {
                notificationService.createNotification(
                    meeting.getCreatedBy().getId(),
                    "📋 Meeting Status Update Needed",
                    String.format(
                        "The meeting \"%s\" has been marked as IN_PROGRESS for over %d hours. " +
                        "If the meeting has concluded, please update its status to record minutes.",
                        meeting.getTitle(),
                        hoursSinceStart
                    ),
                    Notification.NotificationType.DEADLINE_REMINDER,
                    "MEETING",
                    meeting.getId()
                );
            }
        }
    }

    /**
     * Check if a PENDING_MINUTES meeting is still missing its minutes text.
     */
    private boolean isMinutesMissing(Meeting meeting) {
        return meeting.getMinutes() == null || meeting.getMinutes().trim().isEmpty();
    }
}
