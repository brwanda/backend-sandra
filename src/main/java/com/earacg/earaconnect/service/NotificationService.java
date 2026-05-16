package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Notification;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.NotificationRepo;
import com.earacg.earaconnect.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepo notificationRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepo.findByUserIdAndIsRead(userId, false);
    }
    
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepo.countByUserIdAndIsRead(userId, false);
    }
    
    public Notification createNotification(Long userId, String title, String message, 
                                       Notification.NotificationType type, String relatedEntityType, Long relatedEntityId) {
        // First get the user
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepo.save(notification);
    }
    
    public Notification markAsRead(Long notificationId) {
        return notificationRepo.findById(notificationId).map(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            return notificationRepo.save(notification);
        }).orElse(null);
    }
    
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepo.save(notification);
        }
    }
    
    @Autowired
    private EntityHistoryService entityHistoryService;

    public boolean deleteNotification(Long notificationId) {
        java.util.Optional<Notification> notifOpt = notificationRepo.findById(notificationId);
        if (notifOpt.isPresent()) {
            entityHistoryService.recordDeletion("Notification", notificationId, notifOpt.get(), null, null);
            notificationRepo.deleteById(notificationId);
            return true;
        }
        return false;
    }
    
    public void createTaskAssignmentNotification(Long userId, String taskTitle) {
        createNotification(
            userId,
            "Task Assignment",
            "You have been assigned a new task: " + taskTitle,
            Notification.NotificationType.TASK_ASSIGNMENT,
            "Resolution",
            null
        );
    }
    
    public void createReportSubmissionNotification(Long userId, String reportTitle) {
        createNotification(
            userId,
            "Report Submitted",
            "A report has been submitted: " + reportTitle,
            Notification.NotificationType.REPORT_SUBMISSION,
            "Report",
            null
        );
    }
    
    public void createReportApprovalNotification(Long userId, String reportTitle, boolean approved) {
        String title = approved ? "Report Approved" : "Report Rejected";
        String message = "Your report '" + reportTitle + "' has been " + (approved ? "approved" : "rejected") + ".";
        
        createNotification(
            userId,
            title,
            message,
            approved ? Notification.NotificationType.REPORT_APPROVAL : Notification.NotificationType.REPORT_REJECTION,
            "Report",
            null
        );
    }
} 