package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Report;
import com.earacg.earaconnect.model.ReportAttachment;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.service.ReportService;
import com.earacg.earaconnect.service.UserService;
import com.earacg.earaconnect.service.HODPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    private HODPermissionService hodPermissionService;

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable Long id) {
        return reportService.getReportById(id)
                .map(report -> ResponseEntity.ok(report))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resolution/{resolutionId}")
    public ResponseEntity<List<Report>> getReportsByResolution(@PathVariable Long resolutionId) {
        return ResponseEntity.ok(reportService.getReportsByResolution(resolutionId));
    }

    @GetMapping("/subcommittee/{subcommitteeId}")
    public ResponseEntity<List<Report>> getReportsBySubcommittee(@PathVariable Long subcommitteeId) {
        return ResponseEntity.ok(reportService.getReportsBySubcommittee(subcommitteeId));
    }

    @GetMapping("/submitter/{submittedById}")
    public ResponseEntity<List<Report>> getReportsBySubmitter(@PathVariable Long submittedById) {
        return ResponseEntity.ok(reportService.getReportsBySubmitter(submittedById));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Report>> getReportsByStatus(@PathVariable String status) {
        try {
            Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(reportService.getReportsByStatus(reportStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/hod-review/{hodId}")
    public ResponseEntity<List<Report>> getReportsForHodReview(@PathVariable Long hodId) {
        return ResponseEntity.ok(reportService.getReportsForHodReview(hodId));
    }

    @GetMapping("/commissioner-review/{commissionerId}")
    public ResponseEntity<List<Report>> getReportsForCommissionerReview(@PathVariable Long commissionerId) {
        return ResponseEntity.ok(reportService.getReportsForCommissionerReview(commissionerId));
    }

    /** Get reports pending chair review for a specific subcommittee chair */
    @GetMapping("/chair-review/{chairId}")
    public ResponseEntity<List<Report>> getReportsForChairReview(@PathVariable Long chairId) {
        return ResponseEntity.ok(reportService.getReportsForChairReview(chairId));
    }

    @PostMapping
    public ResponseEntity<Report> submitReport(@RequestBody Report report) {
        Report submittedReport = reportService.submitReport(report);
        if (submittedReport != null) {
            return ResponseEntity.ok(submittedReport);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{reportId}/hod-review")
    public ResponseEntity<?> reviewByHod(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> reviewData,
            java.security.Principal principal) {

        try {
            System.out.println("🔍 HOD Review Request - reportId: " + reportId + ", reviewData: " + reviewData);

            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User authenticatedUser = userService.getUserByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            // Never trust a client-sent hodId for authorization.
            if (reviewData.get("hodId") != null) {
                Long requestedHodId = Long.valueOf(reviewData.get("hodId").toString());
                if (!authenticatedUser.getId().equals(requestedHodId)) {
                    return ResponseEntity.status(403)
                            .body(Map.of("error", "You can only act using your own account."));
                }
            }

            String comments = reviewData.get("comments") != null ? reviewData.get("comments").toString() : "";
            boolean hasDecision = reviewData.containsKey("approved") && reviewData.get("approved") != null;
            Integer hodRanking = null;
            if (reviewData.get("hodRanking") != null) {
                hodRanking = Integer.valueOf(reviewData.get("hodRanking").toString());
            }

            if (hasDecision) {
                Boolean approved = Boolean.valueOf(reviewData.get("approved").toString());
                System.out.println("🔍 Parsed review values - hodId: " + authenticatedUser.getId() + ", approved: "
                        + approved + ", comments: " + comments + ", hodRanking: " + hodRanking);

                Report reviewedReport = reportService.reviewByHod(
                        reportId,
                        authenticatedUser.getId(),
                        approved,
                        comments,
                        hodRanking
                );

                if (reviewedReport != null) {
                    System.out.println("✅ HOD review successful");
                    return ResponseEntity.ok(reviewedReport);
                }

                System.err.println("❌ HOD review failed - service returned null");
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Only Chair of HOD can approve or reject reports."));
            } else {
                Report updatedReport = reportService.addHodComment(reportId, authenticatedUser.getId(), comments);
                return ResponseEntity.ok(updatedReport);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error in HOD review: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "HOD review failed: " + e.getMessage()));
        }
    }

    /** Chair reviews individual member ratings and decides whether to forward report to HOD */
    @PostMapping("/{reportId}/chair-review")
    public ResponseEntity<?> reviewByChair(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> reviewData) {
        try {
            if (reviewData.get("chairId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing chairId in request."));
            }
            if (reviewData.get("approved") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing approved in request."));
            }

            Long chairId = Long.valueOf(reviewData.get("chairId").toString());
            Boolean approved = Boolean.valueOf(reviewData.get("approved").toString());
            String comments = reviewData.get("comments") != null ? reviewData.get("comments").toString() : "";
            String memberRatingsSummary = reviewData.get("memberRatingsSummary") != null
                    ? reviewData.get("memberRatingsSummary").toString() : null;

            Report reviewedReport = reportService.reviewByChair(reportId, chairId, approved, comments,
                    memberRatingsSummary);
            if (reviewedReport != null) {
                return ResponseEntity.ok(reviewedReport);
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Chair review failed. Check that the report exists and you have Chair privileges."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chair review failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{reportId}/commissioner-review")
    public ResponseEntity<?> reviewByCommissioner(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> reviewData) {

        try {
            Long commissionerId = Long.valueOf(reviewData.get("commissionerId").toString());
            Boolean approved = Boolean.valueOf(reviewData.get("approved").toString());
            String comments = reviewData.get("comments") != null ? reviewData.get("comments").toString() : "";

            Report reviewedReport = reportService.reviewByCommissioner(reportId, commissionerId, approved, comments);
            if (reviewedReport != null) {
                return ResponseEntity.ok(reviewedReport);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Commissioner review failed"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Commissioner review failed: " + e.getMessage()));
        }
    }

    /**
     * Attach one or more files to an existing report.
     * This does not replace report submission logic; attachments are optional metadata additions.
     */
    @PostMapping("/{reportId}/attachments")
    public ResponseEntity<?> uploadAttachments(
            @PathVariable Long reportId,
            @RequestParam("files") List<MultipartFile> files,
            java.security.Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User user = userService.getUserByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            List<ReportAttachment> saved = reportService.attachFilesToReport(reportId, files, user.getId());
            List<Map<String, Object>> payload = saved.stream()
                    .map(reportService::toAttachmentInfo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload attachments: " + e.getMessage()));
        }
    }

    /** List attachment metadata for a report. */
    @GetMapping("/{reportId}/attachments")
    public ResponseEntity<?> getAttachments(@PathVariable Long reportId) {
        try {
            List<Map<String, Object>> payload = reportService.getReportAttachments(reportId).stream()
                    .map(reportService::toAttachmentInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to load attachments: " + e.getMessage()));
        }
    }

    /** View an attachment inline where browser supports it (pdf/images/text). */
    @GetMapping("/{reportId}/attachments/{attachmentId}/view")
    public ResponseEntity<?> viewAttachment(
            @PathVariable Long reportId,
            @PathVariable Long attachmentId) {
        try {
            ReportAttachment attachment = reportService.getReportAttachment(reportId, attachmentId);
            Resource resource = reportService.loadAttachmentResource(reportId, attachmentId);

            String contentType = attachment.getDocument() != null && attachment.getDocument().getContentType() != null
                    ? attachment.getDocument().getContentType()
                    : "application/octet-stream";
            String filename = attachment.getDocument() != null
                    ? attachment.getDocument().getOriginalFilename()
                    : "attachment";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to view attachment: " + e.getMessage()));
        }
    }

    /** Download an attachment as file. */
    @GetMapping("/{reportId}/attachments/{attachmentId}/download")
    public ResponseEntity<?> downloadAttachment(
            @PathVariable Long reportId,
            @PathVariable Long attachmentId) {
        try {
            ReportAttachment attachment = reportService.getReportAttachment(reportId, attachmentId);
            Resource resource = reportService.loadAttachmentResource(reportId, attachmentId);

            String contentType = attachment.getDocument() != null && attachment.getDocument().getContentType() != null
                    ? attachment.getDocument().getContentType()
                    : "application/octet-stream";
            String filename = attachment.getDocument() != null
                    ? attachment.getDocument().getOriginalFilename()
                    : "attachment";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to download attachment: " + e.getMessage()));
        }
    }

    /**
     * Download report as Excel (.xlsx) or CSV file.
     * Use ?format=csv for CSV, default is Excel.
     * Only accessible by HOD, Commissioner General, Secretary, and Delegation Secretary.
     * Members (COMMITTEE_MEMBER) are NOT allowed to download.
     */
    @GetMapping("/{reportId}/download")
    public ResponseEntity<?> downloadReport(
            @PathVariable Long reportId,
            @RequestParam(required = false, defaultValue = "excel") String format,
            java.security.Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User user = userService.getUserByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Check role-based access: HOD, CG, Secretary, Delegation Secretary, Admin, Chair
            boolean hasHODPrivileges = hodPermissionService.hasHODPrivileges(user);
            User.UserRole role = user.getRole();
            boolean isAllowed = hasHODPrivileges
                    || role == User.UserRole.COMMISSIONER_GENERAL
                    || role == User.UserRole.SECRETARY
                    || role == User.UserRole.COMMITTEE_SECRETARY
                    || role == User.UserRole.DELEGATION_SECRETARY
                    || role == User.UserRole.ADMIN
                    || role == User.UserRole.CHAIR;

            if (!isAllowed) {
                return ResponseEntity.status(403).body(Map.of("error", "You do not have permission to download reports. Only HOD, Commissioner General, Secretaries, and Admin can download reports."));
            }

            Optional<Report> reportOpt = reportService.getReportById(reportId);
            if (reportOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Report report = reportOpt.get();
            String baseName = "report_" + reportId + "_" + report.getSubcommittee().getName().replaceAll("\\s+", "_");

            if ("csv".equalsIgnoreCase(format)) {
                String csvContent = reportService.generateReportCsv(report);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(csvContent);
            } else {
                byte[] excelBytes = reportService.generateReportExcel(report);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(excelBytes);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to download report: " + e.getMessage()));
        }
    }
}