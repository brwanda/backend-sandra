package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    @JsonIgnoreProperties({"invitations", "resolutions", "createdBy", "hostingCountry"})
    private Meeting meeting;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "country", "subcommittee", "resolutions", "meetings"})
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @ManyToOne
    @JoinColumn(name = "recorded_by")
    @JsonIgnoreProperties({"password", "country", "subcommittee", "resolutions", "meetings"})
    private User recordedBy;
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
    
    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        EXCUSED
    }
}