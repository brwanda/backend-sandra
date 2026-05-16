package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entity_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // e.g., "Meeting", "Committee", "Resolution", "Notification", "Attendance"

    @Column(name = "entity_id", nullable = false)
    private Long entityId; // Original entity ID

    @Column(name = "entity_data", columnDefinition = "TEXT", nullable = false)
    private String entityData; // JSON snapshot of the entity before deletion

    @Column(name = "deleted_by")
    private Long deletedBy; // User ID who performed the deletion

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    @Column(name = "deletion_reason")
    private String deletionReason;

    @PrePersist
    protected void onCreate() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }
}
