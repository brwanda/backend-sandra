package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "sub_task_id", nullable = false)
    @JsonIgnoreProperties({"resolution", "meeting", "assignedTo"})
    private SubTask subTask;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"password", "country", "subcommittee", "resolutions", "meetings"})
    private User createdBy;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PlanStatus status = PlanStatus.DRAFT;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private PlanPriority priority = PlanPriority.MEDIUM;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PlanStatus {
        DRAFT,
        ACTIVE,
        ON_HOLD,
        COMPLETED,
        CANCELLED
    }

    public enum PlanPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
