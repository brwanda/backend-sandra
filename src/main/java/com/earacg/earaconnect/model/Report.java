package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "resolution_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "reports", "meeting", "createdBy"})
    private Resolution resolution;

    @ManyToOne
    @JoinColumn(name = "subcommittee_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "members", "reports"})
    private SubCommittee subcommittee;

    @ManyToOne
    @JoinColumn(name = "submitted_by", nullable = false)
    @JsonIgnoreProperties({"resolutions", "meetings", "country", "subcommittee", "reports"})
    private User submittedBy;

    @Column(name = "performance_percentage", nullable = false)
    private Integer performancePercentage;

    @Column(name = "progress_details", columnDefinition = "TEXT")
    private String progressDetails;

    @Column(name = "hindrances", columnDefinition = "TEXT")
    private String hindrances;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.SUBMITTED;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_hod")
    private User reviewedByHod;

    @Column(name = "hod_comments", columnDefinition = "TEXT")
    private String hodComments;

    /** HOD ranking of the report (1-5 scale) when approving */
    @Column(name = "hod_ranking")
    private Integer hodRanking;

    @Column(name = "hod_reviewed_at")
    private LocalDateTime hodReviewedAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_commissioner")
    private User reviewedByCommissioner;

    @Column(name = "commissioner_comments", columnDefinition = "TEXT")
    private String commissionerComments;

    @Column(name = "commissioner_reviewed_at")
    private LocalDateTime commissionerReviewedAt;

    // Chair review fields — Chair reviews member ratings before sending to HOD
    @ManyToOne
    @JoinColumn(name = "reviewed_by_chair")
    private User reviewedByChair;

    @Column(name = "chair_comments", columnDefinition = "TEXT")
    private String chairComments;

    @Column(name = "chair_reviewed_at")
    private LocalDateTime chairReviewedAt;

    @Column(name = "member_ratings_summary", columnDefinition = "TEXT")
    private String memberRatingsSummary;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_final_report", nullable = false)
    private Boolean isFinalReport = false;

    @Column(name = "report_version", nullable = false)
    private Integer reportVersion = 1;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (isFinalReport == null) {
            isFinalReport = false;
        }
        if (reportVersion == null) {
            reportVersion = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReportStatus {
        DRAFT,                      // Initial draft
        PENDING_CHAIR_REVIEW,       // Member submitted → waiting for Chair to review individual ratings
        SUBMITTED,                  // Chair approved & forwarded to HOD (pending HOD review)
        REJECTED_BY_CHAIR,          // Chair rejected → member may revise
        APPROVED_BY_HOD,            // HOD approved, escalated to Commissioner General
        REJECTED_BY_HOD,            // HOD rejected (with comments); Chair may revise and resubmit
        APPROVED_BY_COMMISSIONER,
        REJECTED_BY_COMMISSIONER
    }
} 