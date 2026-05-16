package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "sub_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SubTask {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status = TaskStatus.TODO;

  /** The resolution this sub-task relates to */
  @ManyToOne
  @JoinColumn(name = "resolution_id", nullable = true)
  @JsonIgnoreProperties({ "assignments", "reports", "meeting" })
  private Resolution resolution;

  /** The meeting this sub-task relates to */
  @ManyToOne
  @JoinColumn(name = "meeting_id")
  @JsonIgnoreProperties({ "invitations", "resolutions", "createdBy", "hostingCountry", "minutes", "tasks" })
  private Meeting meeting;

  /** The subcommittee member assigned to do this task */
  @ManyToOne
  @JoinColumn(name = "assigned_to_id")
  @JsonIgnoreProperties({ "subCommittee", "country", "appointedLetterDoc" })
  private CSubCommitteeMembers assignedTo;

  /** ID of the chair who created this task */
  @Column(name = "assigned_by_chair_id")
  private Long assignedByChairId;

  /** The subcommittee this task belongs to */
  @Column(name = "subcommittee_id", nullable = false)
  private Long subcommitteeId;

  /** Target completion deadline for progress tracking */
  @Column(name = "deadline")
  private LocalDateTime deadline;

  /** Source meeting type where this task was created (e.g., TECHNICAL_MEETING) */
  @Column(name = "source_meeting_type", length = 50)
  private String sourceMeetingType;

  /** Flag indicating if this task requires description from Committee Secretary */
  @Column(name = "requires_description")
  private Boolean requiresDescription = false;

  /** Progress note written by the assigned member */
  @Column(name = "progress_note", columnDefinition = "TEXT")
  private String progressNote;

  /** Self-ranking by the member (1-5 scale) before submitting to chair */
  @Column(name = "self_ranking")
  private Integer selfRanking;

  /** Member's description of what is working */
  @Column(name = "working_description", columnDefinition = "TEXT")
  private String workingDescription;

  /** Member's description of what is NOT working */
  @Column(name = "not_working_description", columnDefinition = "TEXT")
  private String notWorkingDescription;

  /** Chair's ranking of the member's work (1-5 scale) */
  @Column(name = "chair_ranking")
  private Integer chairRanking;

  /** Chair's feedback comment on the member's work */
  @Column(name = "chair_feedback", columnDefinition = "TEXT")
  private String chairFeedback;

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

  public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
  }
}
