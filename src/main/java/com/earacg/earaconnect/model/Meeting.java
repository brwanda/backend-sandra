package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "agenda")
    private String agenda;

    @Column(name = "meeting_date", nullable = false)
    private LocalDateTime meetingDate;

    @Column(name = "meeting_end_date")
    private LocalDateTime meetingEndDate;

    @Column(name = "meeting_mode")
    @Enumerated(EnumType.STRING)
    private MeetingMode meetingMode = MeetingMode.PHYSICAL;

    @Column(name = "location")
    private String location;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "invitation_pdf")
    private String invitationPdf;

    @ManyToOne
    @JoinColumn(name = "hosting_country_id", nullable = false)
    @JsonIgnoreProperties({ "eac" })
    private Country hostingCountry;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = true)
    @JsonIgnoreProperties({ "password", "country", "subcommittee", "resolutions", "meetings" })
    private User createdBy;

    @Column(name = "meeting_type")
    @Enumerated(EnumType.STRING)
    private MeetingType meetingType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @Lob
    @Column(name = "minutes", columnDefinition = "TEXT")
    private String minutes;

    @Column(name = "minutes_document")
    private String minutesDocument;

    @Lob
    @Column(name = "aob_items", columnDefinition = "TEXT")
    private String aobItems;

    @Transient
    private Boolean sendNotifications = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id")
    @JsonIgnoreProperties({ "subCommittees", "meetings" })
    private Committee committee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcommittee_id")
    @JsonIgnoreProperties({ "parentCommittee", "meetings" })
    private SubCommittee subCommittee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<MeetingInvitation> invitations;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Resolution> resolutions;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<SubTask> tasks;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MeetingType {
        COMMISSIONER_GENERAL_MEETING,
        TECHNICAL_MEETING,
        SUBCOMMITTEE_MEETING
    }

    public enum MeetingStatus {
        SCHEDULED,
        IN_PROGRESS,
        PENDING_MINUTES,
        COMPLETED,
        CANCELLED
    }

    public enum MeetingMode {
        ONLINE,
        PHYSICAL
    }
}