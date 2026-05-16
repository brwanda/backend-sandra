package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "resolution_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ResolutionAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "resolution_id", nullable = false)
    @JsonBackReference("resolution-assignments")
    private Resolution resolution;

    @ManyToOne
    @JoinColumn(name = "subcommittee_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "members", "reports"})
    private SubCommittee subcommittee;

    @Column(name = "contribution_percentage", nullable = false)
    private Integer contributionPercentage;

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }

    public enum AssignmentStatus {
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
} 