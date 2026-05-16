package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "resolutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Resolution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    @JsonIgnoreProperties({"resolutions", "createdBy", "hostingCountry", "minutes", "invitations", "tasks"})
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = true)
    @JsonIgnoreProperties({"resolutions", "meetings", "country", "subcommittee"})
    private User createdBy;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ResolutionStatus status = ResolutionStatus.ASSIGNED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "resolution", cascade = CascadeType.ALL)
    @JsonManagedReference("resolution-assignments")
    private List<ResolutionAssignment> assignments;

    @OneToMany(mappedBy = "resolution", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"resolution"})
    private List<Report> reports;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ResolutionStatus {
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
} 