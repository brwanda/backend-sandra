package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    List<Notification> findByUserIdAndIsRead(Long userId, boolean isRead);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByType(Notification.NotificationType type);
    long countByUserIdAndIsRead(Long userId, boolean isRead);
    long countByUserId(Long userId);
    boolean existsByUserIdAndTypeAndRelatedEntityTypeAndRelatedEntityIdAndCreatedAtAfter(
            Long userId,
            Notification.NotificationType type,
            String relatedEntityType,
            Long relatedEntityId,
            LocalDateTime createdAt);
    
    /**
     * Delete all notifications for a specific user
     */
    void deleteByUserId(Long userId);
} 