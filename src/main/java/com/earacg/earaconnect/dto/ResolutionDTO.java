package com.earacg.earaconnect.dto;

import com.earacg.earaconnect.model.Resolution;
import com.earacg.earaconnect.model.ResolutionAssignment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class ResolutionDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Meeting info (simplified)
    private Long meetingId;
    private String meetingTitle;
    
    // Created by info (simplified)
    private Long createdById;
    private String createdByName;

    // Assignment info (simplified) – needed by member dashboards
    private List<AssignmentDTO> assignments;

    // Constructors
    public ResolutionDTO() {}

    public ResolutionDTO(Resolution resolution) {
        this.id = resolution.getId();
        this.title = resolution.getTitle();
        this.description = resolution.getDescription();
        this.status = resolution.getStatus() != null ? resolution.getStatus().toString() : null;
        this.createdAt = resolution.getCreatedAt();
        this.updatedAt = resolution.getUpdatedAt();
        
        if (resolution.getMeeting() != null) {
            this.meetingId = resolution.getMeeting().getId();
            this.meetingTitle = resolution.getMeeting().getTitle();
        }
        
        if (resolution.getCreatedBy() != null) {
            this.createdById = resolution.getCreatedBy().getId();
            this.createdByName = resolution.getCreatedBy().getName();
        }

        // Populate assignment info
        if (resolution.getAssignments() != null) {
            this.assignments = new ArrayList<>();
            for (ResolutionAssignment a : resolution.getAssignments()) {
                AssignmentDTO dto = new AssignmentDTO();
                dto.setId(a.getId());
                dto.setContributionPercentage(a.getContributionPercentage());
                dto.setStatus(a.getStatus() != null ? a.getStatus().toString() : null);
                if (a.getSubcommittee() != null) {
                    dto.setSubcommitteeId(a.getSubcommittee().getId());
                    dto.setSubcommitteeName(a.getSubcommittee().getName());
                }
                this.assignments.add(dto);
            }
        }
    }

    /**
     * Build a DTO scoped to a specific subcommittee.
     * Sets the top-level contributionPercentage to the matching assignment's value.
     */
    public ResolutionDTO(Resolution resolution, Long subcommitteeId) {
        this(resolution); // populate all standard fields + assignments
        if (subcommitteeId != null && resolution.getAssignments() != null) {
            resolution.getAssignments().stream()
                .filter(a -> a.getSubcommittee() != null && subcommitteeId.equals(a.getSubcommittee().getId()))
                .findFirst()
                .ifPresent(a -> this.contributionPercentage = a.getContributionPercentage());
        }
    }

    /** Top-level contribution percentage for subcommittee-scoped queries */
    private Integer contributionPercentage;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getMeetingTitle() { return meetingTitle; }
    public void setMeetingTitle(String meetingTitle) { this.meetingTitle = meetingTitle; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public List<AssignmentDTO> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentDTO> assignments) { this.assignments = assignments; }

    public Integer getContributionPercentage() { return contributionPercentage; }
    public void setContributionPercentage(Integer contributionPercentage) { this.contributionPercentage = contributionPercentage; }

    // Nested DTO for safe assignment serialization
    public static class AssignmentDTO {
        private Long id;
        private Long subcommitteeId;
        private String subcommitteeName;
        private Integer contributionPercentage;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getSubcommitteeId() { return subcommitteeId; }
        public void setSubcommitteeId(Long subcommitteeId) { this.subcommitteeId = subcommitteeId; }

        public String getSubcommitteeName() { return subcommitteeName; }
        public void setSubcommitteeName(String subcommitteeName) { this.subcommitteeName = subcommitteeName; }

        public Integer getContributionPercentage() { return contributionPercentage; }
        public void setContributionPercentage(Integer contributionPercentage) { this.contributionPercentage = contributionPercentage; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}