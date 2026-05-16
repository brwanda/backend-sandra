package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.Report;
import com.earacg.earaconnect.model.Resolution;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.repository.UserRepo;
import com.earacg.earaconnect.repository.ReportRepo;
import com.earacg.earaconnect.repository.ResolutionRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ChairValidationService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ReportRepo reportRepo;

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    /**
     * Validate if a user is a Chair
     */
    public boolean isChair(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return User.UserRole.CHAIR.equals(user.getRole()) || 
                   User.UserRole.VICE_CHAIR.equals(user.getRole());
        }
        return false;
    }

    /**
     * Validate if a Chair can access a specific resolution
     */
    @Transactional(readOnly = true)
    public boolean canAccessResolution(Long chairId, Long resolutionId) {
        Optional<User> chairOpt = userRepo.findById(chairId);
        Optional<Resolution> resolutionOpt = resolutionRepo.findById(resolutionId);
        
        if (chairOpt.isPresent() && resolutionOpt.isPresent()) {
            User chair = chairOpt.get();
            Resolution resolution = resolutionOpt.get();
            
            // Check if chair's subcommittee is assigned to this resolution
            // Since Resolution doesn't have getSubcommittees(), we'll check through assignments
            if (resolution.getAssignments() != null) {
                return resolution.getAssignments().stream()
                    .anyMatch(assignment -> assignment.getSubcommittee().getId().equals(chair.getSubcommittee().getId()));
            }
        }
        return false;
    }

    /**
     * Validate if a Chair can submit a report for a resolution
     */
    public boolean canSubmitReport(Long chairId, Long resolutionId) {
        if (!isChair(chairId)) {
            return false;
        }
        
        if (!canAccessResolution(chairId, resolutionId)) {
            return false;
        }
        
        // Check if resolution is in a state that allows report submission
        Optional<Resolution> resolutionOpt = resolutionRepo.findById(resolutionId);
        if (resolutionOpt.isPresent()) {
            Resolution resolution = resolutionOpt.get();
            return "ASSIGNED".equals(resolution.getStatus().name()) || 
                   "IN_PROGRESS".equals(resolution.getStatus().name());
        }
        
        return false;
    }

    /**
     * Validate report data before submission
     */
    public List<String> validateReportData(Report report) {
        List<String> errors = new java.util.ArrayList<>();
        
        if (report.getProgressDetails() == null || report.getProgressDetails().trim().isEmpty()) {
            errors.add("Progress details are required");
        } else if (report.getProgressDetails().trim().length() < 10) {
            errors.add("Progress details must be at least 10 characters long");
        }
        
        if (report.getPerformancePercentage() == null) {
            errors.add("Performance percentage is required");
        } else if (report.getPerformancePercentage() < 0 || report.getPerformancePercentage() > 100) {
            errors.add("Performance percentage must be between 0 and 100");
        }
        
        if (report.getResolution() == null) {
            errors.add("Resolution is required");
        }
        
        if (report.getSubcommittee() == null) {
            errors.add("Subcommittee is required");
        }
        
        return errors;
    }

    /**
     * Get all resolutions assigned to a Chair's subcommittee
     */
    @Transactional(readOnly = true)
    public List<Resolution> getAssignedResolutions(Long chairId) {
        Optional<User> chairOpt = userRepo.findById(chairId);
        if (chairOpt.isPresent()) {
            User chair = chairOpt.get();
            SubCommittee subcommittee = chair.getSubcommittee();
            
            if (subcommittee != null) {
                // Since ResolutionRepo doesn't have findBySubcommitteesContaining,
                // we'll get all resolutions and filter by assignments
                List<Resolution> allResolutions = resolutionRepo.findAll();
                return allResolutions.stream()
                    .filter(resolution -> resolution.getAssignments() != null &&
                            resolution.getAssignments().stream()
                                .anyMatch(assignment -> assignment.getSubcommittee().getId().equals(subcommittee.getId())))
                    .sorted(Comparator
                            .comparing(Resolution::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Resolution::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Resolution::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            }
        }
        return new java.util.ArrayList<>();
    }

    /**
     * Get all reports submitted by this Chair (scope: only their subcommittee).
     */
    @Transactional(readOnly = true)
    public List<Report> getChairReports(Long chairId) {
        Optional<User> chairOpt = userRepo.findById(chairId);
        if (chairOpt.isEmpty() || chairOpt.get().getSubcommittee() == null) {
            return new java.util.ArrayList<>();
        }
        User chair = chairOpt.get();
        Long chairSubcommitteeId = chair.getSubcommittee().getId();
        List<Report> bySubmitter = reportRepo.findBySubmittedById(chairId);
        return bySubmitter.stream()
                .filter(r -> r != null
                        && r.getSubcommittee() != null
                        && r.getSubcommittee().getId() != null
                        && chairSubcommitteeId.equals(r.getSubcommittee().getId()))
            .sorted(Comparator
                .comparing(Report::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Report::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Report::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Validate if a Chair can update a report
     */
    public boolean canUpdateReport(Long chairId, Long reportId) {
        Optional<Report> reportOpt = reportRepo.findById(reportId);
        if (reportOpt.isPresent()) {
            Report report = reportOpt.get();
            return report.getSubmittedBy().getId().equals(chairId) &&
                   "REJECTED_BY_HOD".equals(report.getStatus().name());
        }
        return false;
    }

    /**
     * Get Chair's subcommittee
     */
    public Optional<SubCommittee> getChairSubcommittee(Long chairId) {
        Optional<User> chairOpt = userRepo.findById(chairId);
        if (chairOpt.isPresent()) {
            User chair = chairOpt.get();
            return Optional.ofNullable(chair.getSubcommittee());
        }
        return Optional.empty();
    }

    /**
     * Validate Chair profile update permissions
     */
    public boolean canUpdateProfile(Long chairId, String fieldName) {
        // Chairs can update most fields except role
        return !"role".equalsIgnoreCase(fieldName);
    }
}
