package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.*;
import com.earacg.earaconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class ResolutionService {
    
    @Autowired
    private ResolutionRepo resolutionRepo;
    
    @Autowired
    private ResolutionAssignmentRepo resolutionAssignmentRepo;
    
    @Autowired
    private ReportRepo reportRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private SecretaryValidationService secretaryValidationService;
    
    @Autowired
    private SubCommitteeRepo subCommitteeRepo;
    
    @Autowired
    private EmailService emailService;
    
    public List<Resolution> getAllResolutions() {
        return resolutionRepo.findAll();
    }
    
    public Optional<Resolution> getResolutionById(Long id) {
        return resolutionRepo.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<Resolution> getResolutionsByMeeting(Long meetingId) {
        return resolutionRepo.findByMeetingId(meetingId);
    }
    
    public List<Resolution> getResolutionsByStatus(Resolution.ResolutionStatus status) {
        return resolutionRepo.findByStatus(status);
    }
    
    /**
     * Get all resolutions assigned to a specific subcommittee
     */
    @Transactional(readOnly = true)
    public List<Resolution> getResolutionsBySubcommittee(Long subcommitteeId) {
        // Get all resolutions and filter by assignments
        List<Resolution> allResolutions = resolutionRepo.findAll();
        return allResolutions.stream()
            .filter(resolution -> resolution.getAssignments() != null &&
                    resolution.getAssignments().stream()
                        .anyMatch(assignment -> assignment.getSubcommittee().getId().equals(subcommitteeId)))
            .toList();
    }
    
    public Resolution createResolution(Resolution resolution) {
        resolution.setStatus(Resolution.ResolutionStatus.ASSIGNED);
        resolution.setCreatedAt(LocalDateTime.now());
        return resolutionRepo.save(resolution);
    }
    
    public Resolution updateResolution(Long id, Resolution resolutionDetails) {
        Optional<Resolution> resolutionOpt = resolutionRepo.findById(id);
        if (resolutionOpt.isPresent()) {
            Resolution resolution = resolutionOpt.get();
            resolution.setTitle(resolutionDetails.getTitle());
            resolution.setDescription(resolutionDetails.getDescription());
            resolution.setStatus(resolutionDetails.getStatus());
            resolution.setUpdatedAt(LocalDateTime.now());
            return resolutionRepo.save(resolution);
        }
        return null;
    }
    
    @Autowired
    private EntityHistoryService entityHistoryService;

    public boolean deleteResolution(Long id) {
        java.util.Optional<com.earacg.earaconnect.model.Resolution> resOpt = resolutionRepo.findById(id);
        if (resOpt.isPresent()) {
            entityHistoryService.recordDeletion("Resolution", id, resOpt.get(), null, null);
            resolutionRepo.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Assign resolution to subcommittees with contribution percentages and secretary validation
     */
    public void assignResolutionToSubcommittees(Long resolutionId, List<Map<String, Object>> assignments, Long assignedById) {
        // Validate secretary can assign resolutions
        SecretaryValidationService.ValidationResult validation = 
                secretaryValidationService.validateResolutionAssignment(assignedById);
        
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Cannot assign resolution: " + validation.getMessage());
        }
        
        assignResolutionToSubcommittees(resolutionId, assignments);
    }
    
    /**
     * Assign resolution to subcommittees with contribution percentages (original method)
     */
    @Transactional
    public void assignResolutionToSubcommittees(Long resolutionId, List<Map<String, Object>> assignments) {
        Resolution resolution = resolutionRepo.findById(resolutionId)
                .orElseThrow(() -> new RuntimeException("Resolution not found"));
        
        // Validate total percentage equals 100 (type-safe: handle Integer, Long, Double, String)
        int totalPercentage = assignments.stream()
                .mapToInt(assignment -> {
                    Object pct = assignment.get("contributionPercentage");
                    if (pct instanceof Number) return ((Number) pct).intValue();
                    return Integer.parseInt(pct.toString());
                })
                .sum();
        
        if (totalPercentage != 100) {
            throw new RuntimeException("Total contribution percentage must equal 100%");
        }
        
        // Remove any existing assignments for this resolution first
        List<ResolutionAssignment> existingAssignments = resolutionAssignmentRepo.findByResolutionId(resolutionId);
        if (!existingAssignments.isEmpty()) {
            resolutionAssignmentRepo.deleteAll(existingAssignments);
        }
        
        // Create assignments
        for (Map<String, Object> assignmentData : assignments) {
            ResolutionAssignment assignment = new ResolutionAssignment();
            assignment.setResolution(resolution);
            
            // Get the subcommittee by ID and set it
            Long subcommitteeId = Long.valueOf(assignmentData.get("subcommitteeId").toString());
            SubCommittee subcommittee = subCommitteeRepo.findById(subcommitteeId)
                    .orElseThrow(() -> new RuntimeException("Subcommittee not found with ID: " + subcommitteeId));
            assignment.setSubcommittee(subcommittee);
            
            // Type-safe percentage extraction
            Object pctObj = assignmentData.get("contributionPercentage");
            int pctValue = (pctObj instanceof Number) ? ((Number) pctObj).intValue() : Integer.parseInt(pctObj.toString());
            assignment.setContributionPercentage(pctValue);
            assignment.setAssignedBy(resolution.getCreatedBy() != null ? resolution.getCreatedBy().getId() : 0L);
            assignment.setStatus(ResolutionAssignment.AssignmentStatus.ASSIGNED);
            assignment.setAssignedAt(LocalDateTime.now());
            
            resolutionAssignmentRepo.save(assignment);
            System.out.println("[ResolutionService] Saved assignment: resolution=" + resolutionId + ", subcommittee=" + subcommitteeId + ", percentage=" + pctValue);
            
            // Notify relevant users
            notifySubcommitteeMembers(assignment);
        }
        
        // Update resolution status
        resolution.setStatus(Resolution.ResolutionStatus.IN_PROGRESS);
        resolutionRepo.save(resolution);
        System.out.println("[ResolutionService] Resolution " + resolutionId + " assigned to " + assignments.size() + " subcommittee(s)");
    }
    
    /**
     * Get resolution progress statistics
     */
    public Map<String, Object> getResolutionProgress(Long resolutionId) {
        Map<String, Object> progress = new HashMap<>();
        
        // Get all assignments for this resolution
        List<ResolutionAssignment> assignments = resolutionAssignmentRepo.findByResolutionId(resolutionId);
        
        // Get all reports for this resolution
        List<Report> reports = reportRepo.findByResolutionId(resolutionId);
        
        // Calculate overall progress based on reports
        double overallProgress = reports.stream()
                .mapToDouble(report -> {
                    // Find the assignment for this subcommittee
                    ResolutionAssignment assignment = assignments.stream()
                            .filter(a -> a.getSubcommittee().getId().equals(report.getSubcommittee().getId()))
                            .findFirst()
                            .orElse(null);
                    
                    if (assignment != null) {
                        // Weight the performance by contribution percentage
                        return (report.getPerformancePercentage() * assignment.getContributionPercentage()) / 100.0;
                    }
                    return 0.0;
                })
                .sum();
        
        progress.put("overallProgress", overallProgress);
        progress.put("totalAssignments", assignments.size());
        progress.put("totalReports", reports.size());
        progress.put("assignments", assignments);
        progress.put("reports", reports);
        
        return progress;
    }
    
    private void notifySubcommitteeMembers(ResolutionAssignment assignment) {
        // Get chairs and members of the subcommittee
        List<User> subcommitteeUsers = userRepo.findBySubcommitteeId(assignment.getSubcommittee().getId());
        
        for (User user : subcommitteeUsers) {
            // Send email notification
            try {
                String emailSubject = "New Task Assignment: " + assignment.getResolution().getTitle();
                String emailMessage = "Dear " + user.getName() + ",\n\n" +
                        "A new resolution has been assigned to your subcommittee (" + assignment.getSubcommittee().getName() + ").\n\n" +
                        "Resolution: " + assignment.getResolution().getTitle() + "\n" +
                        "Your subcommittee's contribution: " + assignment.getContributionPercentage() + "%\n\n" +
                        "Please check the EaraConnect system for more details and begin working on this task.\n\n" +
                        "Best regards,\n" +
                        "EaraConnect System Team";
                
                emailService.sendGeneralNotification(
                    user.getEmail(),
                    user.getName(),
                    emailSubject,
                    emailMessage
                );
            } catch (Exception emailError) {
                // Log email error but don't fail the assignment
                System.err.println("Failed to send email to " + user.getEmail() + ": " + emailError.getMessage());
            }
            
            // Create in-system notification
            notificationService.createNotification(
                user.getId(),
                "New Task Assignment",
                "A new resolution has been assigned to your subcommittee: " + assignment.getResolution().getTitle() + 
                " (Contribution: " + assignment.getContributionPercentage() + "%)",
                Notification.NotificationType.TASK_ASSIGNMENT,
                "Resolution",
                assignment.getResolution().getId()
            );
        }
    }
    
    /**
     * Get resolution assignments
     */
    public List<Map<String, Object>> getResolutionAssignments(Long resolutionId) {
        List<ResolutionAssignment> assignments = resolutionAssignmentRepo.findByResolutionId(resolutionId);
        
        return assignments.stream().map(assignment -> {
            Map<String, Object> assignmentMap = new HashMap<>();
            assignmentMap.put("id", assignment.getId());
            assignmentMap.put("resolutionId", assignment.getResolution().getId());
            assignmentMap.put("subcommitteeId", assignment.getSubcommittee().getId());
            assignmentMap.put("subcommitteeName", assignment.getSubcommittee().getName());
            assignmentMap.put("contributionPercentage", assignment.getContributionPercentage());
            assignmentMap.put("assignedBy", assignment.getAssignedBy());
            assignmentMap.put("assignedAt", assignment.getAssignedAt());
            assignmentMap.put("status", assignment.getStatus().toString());
            return assignmentMap;
        }).toList();
    }
    
    /**
     * Create resolution assignments
     */
    @Transactional
    public Map<String, Object> createResolutionAssignments(Long resolutionId, List<Map<String, Object>> assignmentData) {
        Resolution resolution = resolutionRepo.findById(resolutionId)
                .orElseThrow(() -> new RuntimeException("Resolution not found"));
        
        // Clear existing assignments
        List<ResolutionAssignment> existingAssignments = resolutionAssignmentRepo.findByResolutionId(resolutionId);
        resolutionAssignmentRepo.deleteAll(existingAssignments);
        
        List<ResolutionAssignment> newAssignments = new ArrayList<>();
        
        for (Map<String, Object> data : assignmentData) {
            ResolutionAssignment assignment = new ResolutionAssignment();
            assignment.setResolution(resolution);
            
            Long subcommitteeId = Long.valueOf(data.get("subcommitteeId").toString());
            SubCommittee subcommittee = subCommitteeRepo.findById(subcommitteeId)
                    .orElseThrow(() -> new RuntimeException("Subcommittee not found"));
            assignment.setSubcommittee(subcommittee);
            
            // Type-safe extraction
            Object pctObj = data.get("contributionPercentage");
            assignment.setContributionPercentage(
                (pctObj instanceof Number) ? ((Number) pctObj).intValue() : Integer.parseInt(pctObj.toString()));
            Object assignedByObj = data.get("assignedBy");
            assignment.setAssignedBy(
                assignedByObj != null
                    ? (assignedByObj instanceof Number ? ((Number) assignedByObj).longValue() : Long.parseLong(assignedByObj.toString()))
                    : (resolution.getCreatedBy() != null ? resolution.getCreatedBy().getId() : 0L));
            assignment.setStatus(ResolutionAssignment.AssignmentStatus.ASSIGNED);
            assignment.setAssignedAt(LocalDateTime.now());
            
            newAssignments.add(resolutionAssignmentRepo.save(assignment));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Resolution assignments created successfully");
        result.put("assignmentCount", newAssignments.size());
        result.put("resolutionId", resolutionId);
        
        return result;
    }
    
    /**
     * Update resolution status
     */
    public Resolution updateResolutionStatus(Long resolutionId, Resolution.ResolutionStatus status) {
        Optional<Resolution> resolutionOpt = resolutionRepo.findById(resolutionId);
        if (resolutionOpt.isPresent()) {
            Resolution resolution = resolutionOpt.get();
            resolution.setStatus(status);
            resolution.setUpdatedAt(LocalDateTime.now());
            return resolutionRepo.save(resolution);
        }
        return null;
    }
}