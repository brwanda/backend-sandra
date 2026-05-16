package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Report;
import com.earacg.earaconnect.model.Resolution;
import com.earacg.earaconnect.model.SubTask;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.service.ChairValidationService;
import com.earacg.earaconnect.service.ReportService;
import com.earacg.earaconnect.service.UserService;

import lombok.RequiredArgsConstructor;

import com.earacg.earaconnect.service.ResolutionService;
import com.earacg.earaconnect.service.SubCommitteeService;
import com.earacg.earaconnect.repository.SubTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chair")
@RequiredArgsConstructor
public class ChairController {

    @Autowired
    private ChairValidationService chairValidationService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResolutionService resolutionService;

    @Autowired
    private SubCommitteeService subCommitteeService;

    @Autowired
    private SubTaskRepository subTaskRepository;

    /**
     * Get all resolutions assigned to the Chair's subcommittee
     */
    @GetMapping("/resolutions/{chairId}")
    public ResponseEntity<?> getAssignedResolutions(@PathVariable Long chairId) {
        try {
            // First check if user exists
            var userOpt = userService.getUserById(chairId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found with ID: " + chairId));
            }

            User user = userOpt.get();

            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User is not a Chair. Current role: " + user.getRole()));
            }

            List<Resolution> resolutions = chairValidationService.getAssignedResolutions(chairId);

            // Convert to safe DTOs with assignment info to avoid serialization issues
            List<Map<String, Object>> result = resolutions.stream().map(r -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", r.getId());
                dto.put("title", r.getTitle());
                dto.put("description", r.getDescription());
                dto.put("status", r.getStatus() != null ? r.getStatus().name() : null);
                dto.put("createdAt", r.getCreatedAt());
                dto.put("updatedAt", r.getUpdatedAt());
                if (r.getMeeting() != null) {
                    // Frontend expects nested meeting object: resolution.meeting.title
                    Map<String, Object> meetingDto = new HashMap<>();
                    meetingDto.put("id", r.getMeeting().getId());
                    meetingDto.put("title", r.getMeeting().getTitle());
                    dto.put("meeting", meetingDto);
                    dto.put("meetingId", r.getMeeting().getId());
                    dto.put("meetingTitle", r.getMeeting().getTitle());
                }
                if (r.getCreatedBy() != null) {
                    dto.put("createdById", r.getCreatedBy().getId());
                    dto.put("createdByName", r.getCreatedBy().getName());
                }
                if (r.getAssignments() != null) {
                    dto.put("assignments", r.getAssignments().stream().map(a -> {
                        Map<String, Object> aDto = new HashMap<>();
                        aDto.put("id", a.getId());
                        aDto.put("contributionPercentage", a.getContributionPercentage());
                        if (a.getSubcommittee() != null) {
                            // Frontend expects nested subcommittee: a.subcommittee?.id
                            Map<String, Object> subDto = new HashMap<>();
                            subDto.put("id", a.getSubcommittee().getId());
                            subDto.put("name", a.getSubcommittee().getName());
                            aDto.put("subcommittee", subDto);
                        }
                        return aDto;
                    }).toList());
                }
                return dto;
            }).toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error in getAssignedResolutions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch assigned resolutions: " + e.getMessage()));
        }
    }

    /**
     * Get all resolutions assigned to a specific subcommittee
     */
    @GetMapping("/resolutions/subcommittee/{subcommitteeId}")
    public ResponseEntity<?> getResolutionsBySubcommittee(@PathVariable Long subcommitteeId) {
        try {
            List<Resolution> resolutions = resolutionService.getResolutionsBySubcommittee(subcommitteeId);
            // Convert to safe DTOs scoped to this subcommittee (includes contributionPercentage)
            List<com.earacg.earaconnect.dto.ResolutionDTO> dtos = resolutions.stream()
                    .map(r -> new com.earacg.earaconnect.dto.ResolutionDTO(r, subcommitteeId))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch resolutions: " + e.getMessage()));
        }
    }

    /**
     * Get detailed information about a specific resolution
     */
    @GetMapping("/resolutions/{resolutionId}/details")
    public ResponseEntity<?> getResolutionDetails(@PathVariable Long resolutionId, @RequestParam Long chairId) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            if (!chairValidationService.canAccessResolution(chairId, resolutionId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot access this resolution"));
            }

            var resolutionOpt = resolutionService.getResolutionById(resolutionId);
            if (resolutionOpt.isPresent()) {
                return ResponseEntity.ok(resolutionOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch resolution details: " + e.getMessage()));
        }
    }

    /**
     * Submit a new report
     */
    @PostMapping("/reports")
    public ResponseEntity<?> submitReport(@RequestBody Report report, @RequestParam Long chairId) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            if (!chairValidationService.canSubmitReport(chairId, report.getResolution().getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot submit report for this resolution"));
            }

            // Validate report data
            List<String> validationErrors = chairValidationService.validateReportData(report);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Validation failed", "details", validationErrors));
            }

            // Set the submitted by field
            var chairOpt = userService.getUserById(chairId);
            if (chairOpt.isPresent()) {
                report.setSubmittedBy(chairOpt.get());
                report.setStatus(Report.ReportStatus.SUBMITTED);

                // Fix for null Resolution in notifications: Fetch the full Resolution entity
                if (report.getResolution() != null && report.getResolution().getId() != null) {
                    var resolutionOpt = resolutionService.getResolutionById(report.getResolution().getId());
                    if (resolutionOpt.isPresent()) {
                        report.setResolution(resolutionOpt.get());
                        System.out.println("✅ ChairController: Successfully fetched Resolution with ID: "
                                + report.getResolution().getId());
                    } else {
                        System.err.println("❌ ChairController: Resolution not found with ID: "
                                + report.getResolution().getId());
                        return ResponseEntity.badRequest().body(
                                Map.of("error", "Resolution not found with ID: " + report.getResolution().getId()));
                    }
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Resolution is required for report submission"));
                }

                // Fix for TransientPropertyValueException: Fetch the full SubCommittee entity
                if (report.getSubcommittee() != null && report.getSubcommittee().getId() != null) {
                    var subcommitteeOpt = subCommitteeService.getSubCommitteeById(report.getSubcommittee().getId());
                    if (subcommitteeOpt.isPresent()) {
                        report.setSubcommittee(subcommitteeOpt.get());
                        System.out.println("✅ ChairController: Successfully fetched SubCommittee with ID: "
                                + report.getSubcommittee().getId());
                    } else {
                        System.err.println("❌ ChairController: SubCommittee not found with ID: "
                                + report.getSubcommittee().getId());
                        return ResponseEntity.badRequest().body(
                                Map.of("error", "SubCommittee not found with ID: " + report.getSubcommittee().getId()));
                    }
                } else {
                    System.err.println("❌ ChairController: SubCommittee is null or has no ID");
                    // If subcommittee is null or has null ID, try to get it from the chair's
                    // subcommittee
                    if (chairOpt.get().getSubcommittee() != null) {
                        report.setSubcommittee(chairOpt.get().getSubcommittee());
                        System.out.println("✅ ChairController: Using chair's subcommittee: "
                                + chairOpt.get().getSubcommittee().getId());
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("error", "Chair has no assigned subcommittee"));
                    }
                }

                // Auto-calculate performancePercentage from subtask data
                // Uses: task completion rate (60% weight) + chair rankings average (40% weight)
                try {
                    Long subcommitteeId = report.getSubcommittee().getId();
                    Long resolutionId = report.getResolution().getId();
                    List<SubTask> subtasks = subTaskRepository.findBySubcommitteeId(subcommitteeId)
                            .stream()
                            .filter(t -> t.getResolution() != null && t.getResolution().getId().equals(resolutionId))
                            .toList();

                    if (!subtasks.isEmpty()) {
                        // Completion rate: % of tasks marked DONE
                        long totalTasks = subtasks.size();
                        long doneTasks = subtasks.stream()
                                .filter(t -> t.getStatus() == SubTask.TaskStatus.DONE)
                                .count();
                        double completionRate = (double) doneTasks / totalTasks * 100.0;

                        // Chair ranking average (1-5 scale → 0-100%)
                        double chairRankingPct = subtasks.stream()
                                .filter(t -> t.getChairRanking() != null)
                                .mapToInt(SubTask::getChairRanking)
                                .average()
                                .orElse(-1.0);

                        int calculatedPerformance;
                        if (chairRankingPct >= 0) {
                            // Weighted: 60% completion + 40% chair ranking (scaled to 100)
                            double rankingScaled = chairRankingPct / 5.0 * 100.0;
                            calculatedPerformance = (int) Math.round(completionRate * 0.6 + rankingScaled * 0.4);
                        } else if (doneTasks > 0) {
                            // No chair rankings yet — use completion rate only
                            calculatedPerformance = (int) Math.round(completionRate);
                        } else {
                            // No DONE subtasks and no Chair ranking evidence yet.
                            // Keep the manually entered performance instead of forcing 0.
                            calculatedPerformance = report.getPerformancePercentage() != null
                                    ? report.getPerformancePercentage()
                                    : 0;
                        }

                        // Override the manually-entered value with the calculated one
                        report.setPerformancePercentage(Math.max(0, Math.min(100, calculatedPerformance)));
                        System.out.println("✅ ChairController: Auto-calculated performancePercentage = "
                                + calculatedPerformance + "% (completion=" + Math.round(completionRate)
                                + "%, chairRanking=" + (chairRankingPct >= 0 ? Math.round(chairRankingPct * 20) + "%" : "N/A") + ")");

                        // Auto-generate memberRatingsSummary JSON from subtask data
                        try {
                            StringBuilder ratingsJson = new StringBuilder("[");
                            boolean first = true;
                            for (SubTask st : subtasks) {
                                if (!first) ratingsJson.append(",");
                                first = false;
                                ratingsJson.append("{");
                                ratingsJson.append("\"taskId\":").append(st.getId());
                                ratingsJson.append(",\"title\":\"").append(escapeJsonString(st.getTitle())).append("\"");
                                ratingsJson.append(",\"status\":\"").append(st.getStatus()).append("\"");
                                String memberName = (st.getAssignedTo() != null && st.getAssignedTo().getName() != null)
                                        ? st.getAssignedTo().getName() : "Unassigned";
                                ratingsJson.append(",\"memberName\":\"").append(escapeJsonString(memberName)).append("\"");
                                ratingsJson.append(",\"chairRanking\":").append(st.getChairRanking() != null ? st.getChairRanking() : "null");
                                if (st.getProgressNote() != null) {
                                    ratingsJson.append(",\"progressNote\":\"").append(escapeJsonString(st.getProgressNote())).append("\"");
                                }
                                if (st.getWorkingDescription() != null) {
                                    ratingsJson.append(",\"workingDescription\":\"").append(escapeJsonString(st.getWorkingDescription())).append("\"");
                                }
                                if (st.getNotWorkingDescription() != null) {
                                    ratingsJson.append(",\"notWorkingDescription\":\"").append(escapeJsonString(st.getNotWorkingDescription())).append("\"");
                                }
                                if (st.getChairFeedback() != null) {
                                    ratingsJson.append(",\"chairFeedback\":\"").append(escapeJsonString(st.getChairFeedback())).append("\"");
                                }
                                ratingsJson.append("}");
                            }
                            ratingsJson.append("]");
                            report.setMemberRatingsSummary(ratingsJson.toString());
                            System.out.println("✅ ChairController: Auto-generated memberRatingsSummary with " + subtasks.size() + " task entries");
                        } catch (Exception jsonEx) {
                            System.err.println("⚠️ ChairController: Could not generate memberRatingsSummary: " + jsonEx.getMessage());
                        }
                    } else {
                        System.out.println("⚠️ ChairController: No subtasks found for resolution " + resolutionId
                                + " in subcommittee " + subcommitteeId + ", keeping manual performancePercentage = "
                                + report.getPerformancePercentage());
                    }
                } catch (Exception calcEx) {
                    System.err.println("⚠️ ChairController: Could not auto-calculate performance: " + calcEx.getMessage());
                    // Keep the manually-entered value as fallback
                }

                Report savedReport = reportService.submitReport(report);

                // Add success message for HOD confirmation
                Map<String, Object> response = new HashMap<>();
                response.put("id", savedReport.getId());
                response.put("status", savedReport.getStatus());
                response.put("submittedAt", savedReport.getSubmittedAt());
                response.put("resolutionId", savedReport.getResolution() != null ? savedReport.getResolution().getId() : null);
                response.put("subcommitteeId", savedReport.getSubcommittee() != null ? savedReport.getSubcommittee().getId() : null);
                response.put("performancePercentage", savedReport.getPerformancePercentage());
                response.put("progressDetails", savedReport.getProgressDetails());
                response.put("successMessage",
                        "Report submitted successfully! Your report has been sent to HOD for review. Report ID: "
                                + savedReport.getId());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Chair not found"));
            }
        } catch (Exception e) {
            System.err.println("❌ ChairController: Error submitting report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to submit report: " + e.getMessage()));
        }
    }

    /**
     * Get all reports submitted by the Chair
     */
    @GetMapping("/reports/{chairId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getChairReports(@PathVariable Long chairId) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            List<Report> reports = chairValidationService.getChairReports(chairId);
            List<Map<String, Object>> payload = reports.stream().map(report -> {
                Map<String, Object> dto = new HashMap<>();
                Map<String, Object> performanceCalc = buildPerformanceCalculation(report);
                dto.put("id", report.getId());
                dto.put("status", report.getStatus() != null ? report.getStatus().name() : null);
                dto.put("submittedAt", report.getSubmittedAt());
                dto.put("performancePercentage", performanceCalc.get("value"));
                dto.put("performanceCalculation", performanceCalc);
                dto.put("progressDetails", report.getProgressDetails());
                dto.put("hindrances", report.getHindrances());
                dto.put("hodComments", report.getHodComments());
                dto.put("hodReviewedAt", report.getHodReviewedAt());
                dto.put("hodRanking", report.getHodRanking());
                dto.put("commissionerComments", report.getCommissionerComments());

                if (report.getResolution() != null) {
                    Map<String, Object> resolutionDto = new HashMap<>();
                    resolutionDto.put("id", report.getResolution().getId());
                    resolutionDto.put("title", report.getResolution().getTitle());
                    dto.put("resolution", resolutionDto);
                }

                if (report.getSubcommittee() != null) {
                    Map<String, Object> subcommitteeDto = new HashMap<>();
                    subcommitteeDto.put("id", report.getSubcommittee().getId());
                    subcommitteeDto.put("name", report.getSubcommittee().getName());
                    dto.put("subcommittee", subcommitteeDto);
                }

                return dto;
            }).toList();

            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch chair reports: " + e.getMessage()));
        }
    }

    private int resolveEffectiveReportPerformance(Report report) {
        return (int) buildPerformanceCalculation(report).get("value");
    }

    private Map<String, Object> buildPerformanceCalculation(Report report) {
        Map<String, Object> calc = new LinkedHashMap<>();

        if (report == null) {
            calc.put("value", 0);
            calc.put("source", "NO_DATA");
            calc.put("formula", "0 (no report)");
            return calc;
        }

        if (report.getPerformancePercentage() != null && report.getPerformancePercentage() > 0) {
            int value = Math.max(0, Math.min(100, report.getPerformancePercentage()));
            calc.put("value", value);
            calc.put("source", "STORED_REPORT_VALUE");
            calc.put("formula", "Final = stored report performancePercentage");
            calc.put("storedPerformance", report.getPerformancePercentage());
            return calc;
        }

        if (report.getHodRanking() != null && report.getHodRanking() > 0) {
            int rankingPct = report.getHodRanking() * 20;
            int value = Math.max(0, Math.min(100, rankingPct));
            calc.put("value", value);
            calc.put("source", "HOD_RANKING");
            calc.put("formula", "Final = HOD Ranking x 20");
            calc.put("hodRanking", report.getHodRanking());
            return calc;
        }

        if (report.getResolution() != null && report.getResolution().getId() != null
                && report.getSubcommittee() != null && report.getSubcommittee().getId() != null) {
            try {
                List<SubTask> subtasks = subTaskRepository
                        .findByResolutionIdAndSubcommitteeId(report.getResolution().getId(), report.getSubcommittee().getId());
                if (!subtasks.isEmpty()) {
                    long done = subtasks.stream()
                            .filter(t -> t.getStatus() == SubTask.TaskStatus.DONE)
                            .count();
                    int completionPct = (int) Math.round(((double) done / subtasks.size()) * 100.0);
                    int value = Math.max(0, Math.min(100, completionPct));
                    calc.put("value", value);
                    calc.put("source", "SUBTASK_COMPLETION");
                    calc.put("formula", "Final = (Done Subtasks / Total Subtasks) x 100");
                    calc.put("doneSubtasks", done);
                    calc.put("totalSubtasks", subtasks.size());
                    return calc;
                }
            } catch (Exception ignored) {
                // Keep this endpoint resilient and return 0 if fallback fails.
            }
        }

        if (report.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                || report.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER) {
            calc.put("value", 60);
            calc.put("source", "APPROVAL_STATUS_BASELINE");
            calc.put("formula", "Final = 60 (approved report baseline when no numeric evidence exists)");
            return calc;
        }

        calc.put("value", 0);
        calc.put("source", "NO_DATA");
        calc.put("formula", "0 (no stored value, no HOD ranking, no subtasks)");
        return calc;
    }

    /**
     * Update an existing report
     */
    @PutMapping("/reports/{reportId}")
    public ResponseEntity<?> updateReport(@PathVariable Long reportId, @RequestBody Report reportData,
            @RequestParam Long chairId) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            if (!chairValidationService.canUpdateReport(chairId, reportId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot update this report"));
            }

            // Validate report data
            List<String> validationErrors = chairValidationService.validateReportData(reportData);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Validation failed", "details", validationErrors));
            }

            // Fix for TransientPropertyValueException: Fetch the full SubCommittee entity
            if (reportData.getSubcommittee() != null && reportData.getSubcommittee().getId() != null) {
                var subcommitteeOpt = subCommitteeService.getSubCommitteeById(reportData.getSubcommittee().getId());
                if (subcommitteeOpt.isPresent()) {
                    reportData.setSubcommittee(subcommitteeOpt.get());
                } else {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "SubCommittee not found with ID: " + reportData.getSubcommittee().getId()));
                }
            } else {
                // If subcommittee is null or has null ID, try to get it from the chair's
                // subcommittee
                var chairOpt = userService.getUserById(chairId);
                if (chairOpt.isPresent() && chairOpt.get().getSubcommittee() != null) {
                    reportData.setSubcommittee(chairOpt.get().getSubcommittee());
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Chair has no assigned subcommittee"));
                }
            }

            Report updatedReport = reportService.updateReport(reportId, reportData);
            if (updatedReport != null) {
                return ResponseEntity.ok(updatedReport);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Report not found or could not be updated"));
            }
        } catch (Exception e) {
            System.err.println("❌ ChairController: Error updating report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update report: " + e.getMessage()));
        }
    }

    /**
     * Get Chair's profile
     */
    @GetMapping("/profile/{chairId}")
    public ResponseEntity<?> getChairProfile(@PathVariable Long chairId) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            var chairOpt = userService.getUserById(chairId);
            if (chairOpt.isPresent()) {
                return ResponseEntity.ok(chairOpt.get());
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Chair not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch chair profile: " + e.getMessage()));
        }
    }

    /**
     * Update Chair's profile
     */
    @PutMapping("/profile/{chairId}")
    public ResponseEntity<?> updateChairProfile(@PathVariable Long chairId,
            @RequestBody Map<String, Object> profileData) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            // Validate that role is not being updated
            if (profileData.containsKey("role")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role cannot be updated"));
            }

            // For now, we'll return an error since updateUser method doesn't accept Map
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Profile update functionality not implemented yet"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update chair profile: " + e.getMessage()));
        }
    }

    /**
     * Get Chair's subcommittee
     */
    @GetMapping("/subcommittee/{chairId}")
    public ResponseEntity<?> getChairSubcommittee(@PathVariable Long chairId) {
        try {
            if (!chairValidationService.isChair(chairId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
            }

            var subcommitteeOpt = chairValidationService.getChairSubcommittee(chairId);
            if (subcommitteeOpt.isPresent()) {
                return ResponseEntity.ok(subcommitteeOpt.get());
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Chair has no assigned subcommittee"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch chair subcommittee: " + e.getMessage()));
        }
    }

    /**
     * Validate Chair permissions for a specific action
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateChairPermissions(@RequestBody Map<String, Object> request) {
        try {
            Long chairId = Long.valueOf(request.get("chairId").toString());
            String action = request.get("action").toString();
            Long resourceId = request.containsKey("resourceId") ? Long.valueOf(request.get("resourceId").toString())
                    : null;

            Map<String, Object> response = new HashMap<>();
            response.put("isChair", chairValidationService.isChair(chairId));

            switch (action) {
                case "submit_report":
                    if (resourceId != null) {
                        response.put("canSubmit", chairValidationService.canSubmitReport(chairId, resourceId));
                    }
                    break;
                case "access_resolution":
                    if (resourceId != null) {
                        response.put("canAccess", chairValidationService.canAccessResolution(chairId, resourceId));
                    }
                    break;
                case "update_report":
                    if (resourceId != null) {
                        response.put("canUpdate", chairValidationService.canUpdateReport(chairId, resourceId));
                    }
                    break;
                default:
                    response.put("error", "Unknown action");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed: " + e.getMessage()));
        }
    }

    // /test/database endpoint removed — exposed all users and resolutions without auth

    /** Escape a string for safe JSON embedding */
    private String escapeJsonString(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }
}
