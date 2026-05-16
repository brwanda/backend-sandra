package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.*;
import com.earacg.earaconnect.repository.ReportAttachmentRepo;
import com.earacg.earaconnect.repository.ReportRepo;
import com.earacg.earaconnect.repository.UserRepo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportService {
    
    @Autowired
    private ReportRepo reportRepo;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private HODPermissionService hodPermissionService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ReportAttachmentRepo reportAttachmentRepo;
    
    public List<Report> getAllReports() {
        return sortReportsNewestFirst(reportRepo.findAll());
    }
    
    @Transactional(readOnly = true)
    public Optional<Report> getReportById(Long id) {
        return reportRepo.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<Report> getReportsByResolution(Long resolutionId) {
        return sortReportsNewestFirst(reportRepo.findByResolutionId(resolutionId));
    }
    
    @Transactional(readOnly = true)
    public List<Report> getReportsBySubcommittee(Long subcommitteeId) {
        return sortReportsNewestFirst(reportRepo.findBySubcommitteeId(subcommitteeId));
    }
    
    public List<Report> getReportsBySubmitter(Long submittedById) {
        return sortReportsNewestFirst(reportRepo.findBySubmittedById(submittedById));
    }
    
    @Transactional(readOnly = true)
    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        return sortReportsNewestFirst(reportRepo.findByStatus(status));
    }
    
    /**
     * Reports visible to HOD for their country (all statuses).
     * Approval actions remain status-gated to SUBMITTED in reviewByHod.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsForHodReview(Long hodId) {
        Optional<User> hodOpt = userRepo.findById(hodId);
        if (hodOpt.isEmpty() || hodOpt.get().getCountry() == null) {
            return List.of();
        }
        Long countryId = hodOpt.get().getCountry().getId();
        return sortReportsNewestFirst(reportRepo.findAll().stream()
                .filter(r -> r.getSubmittedBy() != null && r.getSubmittedBy().getCountry() != null
                        && r.getSubmittedBy().getCountry().getId().equals(countryId))
            .collect(Collectors.toList()));
    }

    /**
     * Reports escalated to Commissioner General: status APPROVED_BY_HOD (pending CG review).
     * CG sees all such reports globally.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsForCommissionerReview(Long commissionerId) {
        return sortReportsNewestFirst(reportRepo.findByStatus(Report.ReportStatus.APPROVED_BY_HOD));
    }
    
    @Transactional
    public Report submitReport(Report report) {
        try {
            System.out.println("🔍 ReportService.submitReport - Starting report submission");
            System.out.println("   Resolution ID: " + (report.getResolution() != null ? report.getResolution().getId() : "NULL"));
            System.out.println("   SubCommittee ID: " + (report.getSubcommittee() != null ? report.getSubcommittee().getId() : "NULL"));
            System.out.println("   Submitted By: " + (report.getSubmittedBy() != null ? report.getSubmittedBy().getId() : "NULL"));
            
            report.setStatus(Report.ReportStatus.SUBMITTED);
            report.setSubmittedAt(LocalDateTime.now());
            
            Report savedReport = reportRepo.save(report);
            System.out.println("✅ ReportService.submitReport - Report saved successfully with ID: " + savedReport.getId());
            
            // Notify HODs about new report submission
            notifyHodsAboutReport(savedReport);
            
            return savedReport;
        } catch (Exception e) {
            System.err.println("❌ ReportService.submitReport - Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to submit report: " + e.getMessage(), e);
        }
    }
    
    public Report reviewByHod(Long reportId, Long hodId, boolean approved, String comments) {
        return reviewByHod(reportId, hodId, approved, comments, null);
    }

    public Report reviewByHod(Long reportId, Long hodId, boolean approved, String comments, Integer hodRanking) {
        System.out.println("🔍 ReportService.reviewByHod - reportId: " + reportId + ", hodId: " + hodId + ", approved: " + approved);
        
        Optional<Report> reportOpt = reportRepo.findById(reportId);
        if (reportOpt.isEmpty()) {
            System.err.println("❌ Report not found with ID: " + reportId);
            return null;
        }
        
        Report report = reportOpt.get();
        System.out.println("✅ Found report: " + report.getId() + " with status: " + report.getStatus());
        
        // Status gate: only SUBMITTED reports can be reviewed by HOD
        if (report.getStatus() != Report.ReportStatus.SUBMITTED) {
            throw new IllegalStateException("Report must be in SUBMITTED status for HOD review. Current status: " + report.getStatus());
        }
        
        Optional<User> hodOpt = userRepo.findById(hodId);
        if (hodOpt.isEmpty()) {
            System.err.println("❌ HOD user not found with ID: " + hodId);
            return null;
        }
        
        User hod = hodOpt.get();
        System.out.println("✅ Found HOD user: " + hod.getName() + " with role: " + hod.getRole());
        
        // Validate that the user can perform approve/reject (Chair-designated HOD only)
        boolean canApproveOrReject = hodPermissionService.canApproveOrRejectReports(hod);
        System.out.println("🔍 HOD approve/reject permission check: " + canApproveOrReject);

        if (!canApproveOrReject) {
            System.err.println("❌ User " + hod.getName() + " is not allowed to approve/reject reports");
            return null;
        }
        
        // Country gate: HOD can only review reports from submitters in the same country
        if (hod.getCountry() != null && report.getSubmittedBy() != null 
                && report.getSubmittedBy().getCountry() != null
                && !hod.getCountry().getId().equals(report.getSubmittedBy().getCountry().getId())) {
            throw new IllegalArgumentException("HOD can only review reports from their own country");
        }
            report.setReviewedByHod(hod);
            report.setHodComments(comments);
            report.setHodReviewedAt(LocalDateTime.now());
            
            if (approved) {
                // Validate ranking is provided when approving
                if (hodRanking != null) {
                    if (hodRanking < 1 || hodRanking > 5) {
                        throw new IllegalArgumentException("HOD ranking must be between 1 and 5.");
                    }
                    report.setHodRanking(hodRanking);
                    if (report.getPerformancePercentage() == null || report.getPerformancePercentage() <= 0) {
                        report.setPerformancePercentage(Math.max(0, Math.min(100, hodRanking * 20)));
                    }
                }
                report.setStatus(Report.ReportStatus.APPROVED_BY_HOD);
                System.out.println("✅ Report APPROVED by HOD");
                // Forward to Commissioner General
                notifyCommissionerAboutReport(report);
                // Notify Chair about approval
                notifyChairAboutApproval(report);
            } else {
                // Rejection must include a comment so Chair knows what to fix
                if (comments == null || comments.trim().isEmpty()) {
                    throw new IllegalArgumentException("Rejection comment is required when rejecting a report. Please provide feedback for the Chair.");
                }
                report.setStatus(Report.ReportStatus.REJECTED_BY_HOD);
                report.setHodComments(comments);
                System.out.println("❌ Report REJECTED by HOD");
                // Notify Chair about rejection
                notifyChairAboutRejection(report);
            }
            
            Report savedReport = reportRepo.save(report);
            System.out.println("✅ Report saved with new status: " + savedReport.getStatus());
            return savedReport;
    }

    @Transactional
    public Report addHodComment(Long reportId, Long hodId, String comments) {
        if (comments == null || comments.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment is required.");
        }

        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + reportId));

        User hod = userRepo.findById(hodId)
                .orElseThrow(() -> new IllegalArgumentException("HOD user not found with ID: " + hodId));

        if (!hodPermissionService.canCommentOnReports(hod)) {
            throw new IllegalArgumentException("You do not have permission to comment on this report.");
        }

        // Country gate: HOD can only comment on reports from submitters in the same country
        if (hod.getCountry() != null && report.getSubmittedBy() != null
                && report.getSubmittedBy().getCountry() != null
                && !hod.getCountry().getId().equals(report.getSubmittedBy().getCountry().getId())) {
            throw new IllegalArgumentException("HOD can only comment on reports from their own country");
        }

        String commentEntry = "[" + LocalDateTime.now() + "] " + hod.getName() + ": " + comments.trim();
        String existing = report.getHodComments();
        report.setHodComments(existing == null || existing.isBlank()
                ? commentEntry
                : existing + "\n\n" + commentEntry);

        if (report.getReviewedByHod() == null) {
            report.setReviewedByHod(hod);
        }
        report.setHodReviewedAt(LocalDateTime.now());

        Report saved = reportRepo.save(report);
        notifyStakeholdersAboutHodComment(saved, hod, comments.trim());
        return saved;
    }

    /**
     * Comment-only updates should inform report stakeholders, not trigger commissioner review noise.
     * Recipients:
     * 1) Report submitter (Chair)
     * 2) Chair-designated HOD users in the same country (if different from commenter)
     */
    private void notifyStakeholdersAboutHodComment(Report report, User commentingHod, String commentText) {
        try {
            java.util.Set<Long> notified = new java.util.HashSet<>();

            // Notify report submitter/chair first.
            User submitter = report.getSubmittedBy();
            if (submitter != null && submitter.getId() != null && !submitter.getId().equals(commentingHod.getId())) {
                sendHodCommentNotification(submitter, report, commentingHod, commentText);
                notified.add(submitter.getId());
            }

            // Notify HOD Chairs in same country for oversight coordination.
            List<User> chairs = userRepo.findByRole(User.UserRole.CHAIR);
            for (User chair : chairs) {
                if (chair == null || chair.getId() == null) continue;
                if (notified.contains(chair.getId()) || chair.getId().equals(commentingHod.getId())) continue;
                if (!hodPermissionService.isHODChair(chair)) continue;
                if (!isSameCountry(chair, submitter)) continue;

                sendHodCommentNotification(chair, report, commentingHod, commentText);
                notified.add(chair.getId());
            }
        } catch (Exception e) {
            System.err.println("Error notifying stakeholders about HOD comment: " + e.getMessage());
        }
    }

    private boolean isSameCountry(User left, User right) {
        if (left == null || right == null || left.getCountry() == null || right.getCountry() == null) {
            return false;
        }
        return left.getCountry().getId() != null
                && left.getCountry().getId().equals(right.getCountry().getId());
    }

    private void sendHodCommentNotification(User recipient, Report report, User commentingHod, String commentText) {
        notificationService.createNotification(
            recipient.getId(),
            "New HOD Comment",
            "" + commentingHod.getName() + " commented on report for '" + report.getResolution().getTitle() + "': " + commentText,
            Notification.NotificationType.REPORT_COMMENT,
            "Report",
            report.getId()
        );

        try {
            emailService.sendReportNotification(
                recipient.getEmail(),
                recipient.getName(),
                report.getResolution().getTitle(),
                "updated with a comment by Head of Delegation"
            );
        } catch (Exception e) {
            System.err.println("Failed to send comment email to " + recipient.getEmail() + ": " + e.getMessage());
        }
    }
    
    public Report reviewByCommissioner(Long reportId, Long commissionerId, boolean approved, String comments) {
        Optional<Report> reportOpt = reportRepo.findById(reportId);
        if (reportOpt.isPresent()) {
            Report report = reportOpt.get();
            
            // Status gate: only APPROVED_BY_HOD reports can be reviewed by Commissioner
            if (report.getStatus() != Report.ReportStatus.APPROVED_BY_HOD) {
                throw new IllegalStateException("Report must be APPROVED_BY_HOD for Commissioner review. Current status: " + report.getStatus());
            }
            
            User commissioner = userRepo.findById(commissionerId).orElse(null);
            
            // Validate caller is actually a Commissioner General
            if (commissioner == null || commissioner.getRole() != User.UserRole.COMMISSIONER_GENERAL) {
                throw new IllegalArgumentException("Only a Commissioner General can perform this review");
            }

            report.setReviewedByCommissioner(commissioner);
            report.setCommissionerComments(comments);
            report.setCommissionerReviewedAt(LocalDateTime.now());

            if (approved) {
                report.setStatus(Report.ReportStatus.APPROVED_BY_COMMISSIONER);
            } else {
                report.setStatus(Report.ReportStatus.REJECTED_BY_COMMISSIONER);
            }

            return reportRepo.save(report);
        }
        return null;
    }
    
    /**
     * Update an existing report (for resubmission after rejection)
     */
    public Report updateReport(Long reportId, Report reportDetails) {
        Optional<Report> reportOpt = reportRepo.findById(reportId);
        if (reportOpt.isPresent()) {
            Report report = reportOpt.get();

            // Chair can only revise and resubmit when HOD did not approve (rejected).
            if (report.getStatus() != Report.ReportStatus.REJECTED_BY_HOD) {
                throw new IllegalStateException(
                    "Report can only be edited/resubmitted after HOD rejection. Current status: " + report.getStatus()
                );
            }
            
            // Update report details
            report.setProgressDetails(reportDetails.getProgressDetails());
            report.setHindrances(reportDetails.getHindrances());
            report.setPerformancePercentage(reportDetails.getPerformancePercentage());
            report.setSubmittedAt(LocalDateTime.now());
            report.setReportVersion((report.getReportVersion() == null ? 1 : report.getReportVersion()) + 1);
            
            // Resubmit to HOD review queue
            report.setStatus(Report.ReportStatus.SUBMITTED);
            report.setReviewedByCommissioner(null);
            report.setCommissionerComments(null);
            report.setCommissionerReviewedAt(null);
            
            Report savedReport = reportRepo.save(report);
            
            // Notify HODs about resubmitted report
            notifyHodsAboutReport(savedReport);
            
            return savedReport;
        }
        return null;
    }
    
    /**
     * Generate downloadable text content for a report.
     */
    public String generateReportDownloadContent(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("                    EARA CONNECT - REPORT\n");
        sb.append("═══════════════════════════════════════════════════════\n\n");

        sb.append("Report ID:            ").append(report.getId()).append("\n");
        sb.append("Status:               ").append(report.getStatus()).append("\n");
        sb.append("Submitted At:         ").append(report.getSubmittedAt()).append("\n");
        sb.append("Report Version:       ").append(report.getReportVersion()).append("\n\n");

        sb.append("── Resolution ──\n");
        if (report.getResolution() != null) {
            sb.append("Title:                ").append(report.getResolution().getTitle()).append("\n");
            sb.append("Description:          ").append(report.getResolution().getDescription() != null ? report.getResolution().getDescription() : "N/A").append("\n");
        }
        sb.append("\n");

        sb.append("── Subcommittee ──\n");
        if (report.getSubcommittee() != null) {
            sb.append("Name:                 ").append(report.getSubcommittee().getName()).append("\n");
        }
        sb.append("\n");

        sb.append("── Submitted By ──\n");
        if (report.getSubmittedBy() != null) {
            sb.append("Chair:                ").append(report.getSubmittedBy().getName()).append("\n");
            sb.append("Email:                ").append(report.getSubmittedBy().getEmail()).append("\n");
        }
        sb.append("\n");

        sb.append("── Performance ──\n");
        sb.append("Performance %:        ").append(report.getPerformancePercentage()).append("%\n\n");

        sb.append("── Progress Details ──\n");
        sb.append(report.getProgressDetails() != null ? report.getProgressDetails() : "N/A").append("\n\n");

        sb.append("── Hindrances ──\n");
        sb.append(report.getHindrances() != null && !report.getHindrances().isEmpty() ? report.getHindrances() : "None reported").append("\n\n");

        if (report.getReviewedByHod() != null) {
            sb.append("── HOD Review ──\n");
            sb.append("Reviewed By:          ").append(report.getReviewedByHod().getName()).append("\n");
            sb.append("Review Date:          ").append(report.getHodReviewedAt()).append("\n");
            sb.append("HOD Ranking:          ").append(report.getHodRanking() != null ? report.getHodRanking() + "/5" : "N/A").append("\n");
            sb.append("HOD Comments:         ").append(report.getHodComments() != null ? report.getHodComments() : "None").append("\n\n");
        }

        if (report.getReviewedByCommissioner() != null) {
            sb.append("── Commissioner Review ──\n");
            sb.append("Reviewed By:          ").append(report.getReviewedByCommissioner().getName()).append("\n");
            sb.append("Review Date:          ").append(report.getCommissionerReviewedAt()).append("\n");
            sb.append("Comments:             ").append(report.getCommissionerComments() != null ? report.getCommissionerComments() : "None").append("\n\n");
        }

        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("        Generated by EARA Connect System\n");
        sb.append("═══════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    /**
     * Generate a CSV file for a single report. Works in all Excel versions.
     */
    public String generateReportCsv(Report report) {
        StringBuilder sb = new StringBuilder();
        // BOM for UTF-8 Excel compatibility
        sb.append("\uFEFF");

        sb.append("Section,Field,Value\n");

        sb.append("Report Info,Report ID,").append(report.getId()).append("\n");
        sb.append("Report Info,Status,").append(report.getStatus()).append("\n");
        sb.append("Report Info,Submitted At,").append(report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : "N/A").append("\n");
        sb.append("Report Info,Report Version,").append(report.getReportVersion()).append("\n");

        if (report.getResolution() != null) {
            sb.append("Resolution,Title,\"").append(escapeCsv(report.getResolution().getTitle())).append("\"\n");
            sb.append("Resolution,Description,\"").append(escapeCsv(report.getResolution().getDescription())).append("\"\n");
        }

        if (report.getSubcommittee() != null) {
            sb.append("Subcommittee,Name,").append(report.getSubcommittee().getName()).append("\n");
        }

        if (report.getSubmittedBy() != null) {
            sb.append("Submitted By,Chair,").append(report.getSubmittedBy().getName()).append("\n");
            sb.append("Submitted By,Email,").append(report.getSubmittedBy().getEmail()).append("\n");
        }

        sb.append("Performance,Performance %,").append(report.getPerformancePercentage()).append("%\n");
        sb.append("Performance,Progress Details,\"").append(escapeCsv(report.getProgressDetails())).append("\"\n");
        sb.append("Performance,Hindrances,\"").append(escapeCsv(report.getHindrances())).append("\"\n");

        if (report.getReviewedByHod() != null) {
            sb.append("HOD Review,Reviewed By,").append(report.getReviewedByHod().getName()).append("\n");
            sb.append("HOD Review,Review Date,").append(report.getHodReviewedAt() != null ? report.getHodReviewedAt().toString() : "N/A").append("\n");
            sb.append("HOD Review,HOD Ranking,").append(report.getHodRanking() != null ? report.getHodRanking() + "/5" : "N/A").append("\n");
            sb.append("HOD Review,Comments,\"").append(escapeCsv(report.getHodComments())).append("\"\n");
        }

        if (report.getReviewedByCommissioner() != null) {
            sb.append("Commissioner Review,Reviewed By,").append(report.getReviewedByCommissioner().getName()).append("\n");
            sb.append("Commissioner Review,Review Date,").append(report.getCommissionerReviewedAt() != null ? report.getCommissionerReviewedAt().toString() : "N/A").append("\n");
            sb.append("Commissioner Review,Comments,\"").append(escapeCsv(report.getCommissionerComments())).append("\"\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) return "N/A";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }

    /**
     * Generate an Excel (.xlsx) file for a single report.
     */
    @Transactional(readOnly = true)
    public byte[] generateReportExcel(Report report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            // -- Styles --
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelFont.setFontHeightInPoints((short) 11);
            labelStyle.setFont(labelFont);
            labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            labelStyle.setBorderBottom(BorderStyle.THIN);
            labelStyle.setBorderTop(BorderStyle.THIN);

            CellStyle valueStyle = workbook.createCellStyle();
            valueStyle.setWrapText(true);
            valueStyle.setVerticalAlignment(VerticalAlignment.TOP);

            CellStyle sectionStyle = workbook.createCellStyle();
            Font sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 12);
            sectionFont.setColor(IndexedColors.WHITE.getIndex());
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionStyle.setAlignment(HorizontalAlignment.CENTER);

            // Set column widths
            sheet.setColumnWidth(0, 7000);
            sheet.setColumnWidth(1, 15000);

            int rowNum = 0;

            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("EARA CONNECT - REPORT");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
            rowNum++; // blank row

            // -- Report Info Section --
            rowNum = addSectionHeader(sheet, rowNum, "Report Information", sectionStyle);
            rowNum = addRow(sheet, rowNum, "Report ID", String.valueOf(report.getId()), labelStyle, valueStyle);
            rowNum = addRow(sheet, rowNum, "Status", String.valueOf(report.getStatus()), labelStyle, valueStyle);
            rowNum = addRow(sheet, rowNum, "Submitted At", report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : "N/A", labelStyle, valueStyle);
            rowNum = addRow(sheet, rowNum, "Report Version", String.valueOf(report.getReportVersion()), labelStyle, valueStyle);
            rowNum++;

            // -- Resolution --
            rowNum = addSectionHeader(sheet, rowNum, "Resolution", sectionStyle);
            if (report.getResolution() != null) {
                rowNum = addRow(sheet, rowNum, "Title", report.getResolution().getTitle(), labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "Description", report.getResolution().getDescription() != null ? report.getResolution().getDescription() : "N/A", labelStyle, valueStyle);
            }
            rowNum++;

            // -- Subcommittee --
            rowNum = addSectionHeader(sheet, rowNum, "Subcommittee", sectionStyle);
            if (report.getSubcommittee() != null) {
                rowNum = addRow(sheet, rowNum, "Name", report.getSubcommittee().getName(), labelStyle, valueStyle);
            }
            rowNum++;

            // -- Submitted By --
            rowNum = addSectionHeader(sheet, rowNum, "Submitted By", sectionStyle);
            if (report.getSubmittedBy() != null) {
                rowNum = addRow(sheet, rowNum, "Chair", report.getSubmittedBy().getName(), labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "Email", report.getSubmittedBy().getEmail(), labelStyle, valueStyle);
            }
            rowNum++;

            // -- Performance --
            rowNum = addSectionHeader(sheet, rowNum, "Performance", sectionStyle);
            rowNum = addRow(sheet, rowNum, "Performance %", report.getPerformancePercentage() + "%", labelStyle, valueStyle);
            rowNum = addRow(sheet, rowNum, "Progress Details", report.getProgressDetails() != null ? report.getProgressDetails() : "N/A", labelStyle, valueStyle);
            rowNum = addRow(sheet, rowNum, "Hindrances", report.getHindrances() != null && !report.getHindrances().isEmpty() ? report.getHindrances() : "None reported", labelStyle, valueStyle);
            rowNum++;

            // -- HOD Review --
            if (report.getReviewedByHod() != null) {
                rowNum = addSectionHeader(sheet, rowNum, "HOD Review", sectionStyle);
                rowNum = addRow(sheet, rowNum, "Reviewed By", report.getReviewedByHod().getName(), labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "Review Date", report.getHodReviewedAt() != null ? report.getHodReviewedAt().toString() : "N/A", labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "HOD Ranking", report.getHodRanking() != null ? report.getHodRanking() + "/5" : "N/A", labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "HOD Comments", report.getHodComments() != null ? report.getHodComments() : "None", labelStyle, valueStyle);
                rowNum++;
            }

            // -- Commissioner Review --
            if (report.getReviewedByCommissioner() != null) {
                rowNum = addSectionHeader(sheet, rowNum, "Commissioner Review", sectionStyle);
                rowNum = addRow(sheet, rowNum, "Reviewed By", report.getReviewedByCommissioner().getName(), labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "Review Date", report.getCommissionerReviewedAt() != null ? report.getCommissionerReviewedAt().toString() : "N/A", labelStyle, valueStyle);
                rowNum = addRow(sheet, rowNum, "Comments", report.getCommissionerComments() != null ? report.getCommissionerComments() : "None", labelStyle, valueStyle);
                rowNum++;
            }

            // Footer
            Row footerRow = sheet.createRow(rowNum);
            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue("Generated by EARA Connect System");
            footerCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private int addSectionHeader(Sheet sheet, int rowNum, String title, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        Cell cell2 = row.createCell(1);
        cell2.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 1));
        return rowNum + 1;
    }

    private int addRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(valueStyle);
        return rowNum + 1;
    }

    private void notifyHodsAboutReport(Report report) {
        // Get all HOD-related users.
        List<User> hodRoleUsers = userRepo.findByRole(User.UserRole.HOD);
        List<User> chairs = userRepo.findByRole(User.UserRole.CHAIR);
        List<User> viceChairs = userRepo.findByRole(User.UserRole.VICE_CHAIR);

        // Combine all potential HOD users.
        List<User> allPotentialHods = new java.util.ArrayList<>(hodRoleUsers);
        allPotentialHods.addAll(chairs);
        allPotentialHods.addAll(viceChairs);

        java.util.Set<Long> notified = new java.util.HashSet<>();

        // Filter and notify only those with HOD privileges.
        for (User user : allPotentialHods) {
            if (user.getId() != null && notified.add(user.getId()) && hodPermissionService.hasHODPrivileges(user)) {
                // Create in-app notification
                notificationService.createNotification(
                    user.getId(),
                    "New Report Submission",
                    "A new report has been submitted for '" + report.getResolution().getTitle() + "' by " + report.getSubmittedBy().getName(),
                    Notification.NotificationType.REPORT_SUBMISSION,
                    "Report",
                    report.getId()
                );
                
                // Send email notification
                try {
                    emailService.sendReportNotification(
                        user.getEmail(),
                        user.getName(),
                        report.getResolution().getTitle(),
                        "submitted for your review"
                    );
                } catch (Exception e) {
                    System.err.println("Failed to send email to HOD " + user.getEmail() + ": " + e.getMessage());
                }
            }
        }
    }
    
    private void notifyCommissionerAboutReport(Report report) {
        List<User> commissioners = userRepo.findByRole(User.UserRole.COMMISSIONER_GENERAL);
        for (User commissioner : commissioners) {
            // Create in-app notification
            notificationService.createNotification(
                commissioner.getId(),
                "Report Approved by HOD",
                "A report for '" + report.getResolution().getTitle() + "' has been approved by HOD and forwarded for final review",
                Notification.NotificationType.REPORT_APPROVAL,
                "Report",
                report.getId()
            );
            
            // Send email notification
            try {
                emailService.sendReportNotification(
                    commissioner.getEmail(),
                    commissioner.getName(),
                    report.getResolution().getTitle(),
                    "approved by HOD and forwarded for final review"
                );
            } catch (Exception e) {
                System.err.println("Failed to send email to Commissioner " + commissioner.getEmail() + ": " + e.getMessage());
            }
        }
    }
    
    private void notifyChairAboutRejection(Report report) {
        User chair = report.getSubmittedBy();
        
        // Create in-app notification
        notificationService.createNotification(
            chair.getId(),
            "Report Rejected",
            "Your report for '" + report.getResolution().getTitle() + "' has been rejected. Comments: " + report.getHodComments(),
            Notification.NotificationType.REPORT_REJECTION,
            "Report",
            report.getId()
        );
        
        // Send email notification
        try {
            emailService.sendReportRejectionNotification(
                chair.getEmail(),
                chair.getName(),
                report.getResolution().getTitle(),
                report.getHodComments(),
                report.getReviewedByHod().getName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send rejection email to Chair " + chair.getEmail() + ": " + e.getMessage());
        }
    }
    
    private void notifyChairAboutApproval(Report report) {
        User chair = report.getSubmittedBy();
        
        // Create in-app notification
        notificationService.createNotification(
            chair.getId(),
            "Report Approved",
            "Your report for '" + report.getResolution().getTitle() + "' has been approved and forwarded to Commissioner General",
            Notification.NotificationType.REPORT_APPROVAL,
            "Report",
            report.getId()
        );
        
        // Send email notification
        try {
            emailService.sendReportApprovalNotification(
                chair.getEmail(),
                chair.getName(),
                report.getResolution().getTitle(),
                report.getHodComments(),
                report.getReviewedByHod().getName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send approval email to Chair " + chair.getEmail() + ": " + e.getMessage());
        }
    }

    // ── Chair Review Workflow ──────────────────────────────────────────────────

    /**
     * Get reports pending chair review (status = PENDING_CHAIR_REVIEW) for a given chair's subcommittee.
     */
    public List<Report> getReportsForChairReview(Long chairId) {
        User chair = userRepo.findById(chairId).orElse(null);
        if (chair == null) return List.of();

        List<Report> pending = reportRepo.findByStatus(Report.ReportStatus.PENDING_CHAIR_REVIEW);
        // If chair is associated with a subcommittee, filter to that subcommittee
        if (chair.getSubcommittee() != null) {
            Long subId = chair.getSubcommittee().getId();
            return sortReportsNewestFirst(pending.stream()
                    .filter(r -> r.getSubcommittee() != null && r.getSubcommittee().getId().equals(subId))
                    .collect(java.util.stream.Collectors.toList()));
        }
        return sortReportsNewestFirst(pending);
    }

    /**
     * Chair reviews individual member ratings and decides whether to forward the report to the HOD.
     * If approved, the report status moves to SUBMITTED (visible to HOD).
     * If rejected, status moves to REJECTED_BY_CHAIR so the member can revise.
     */
    @Transactional
    public Report reviewByChair(Long reportId, Long chairId, boolean approved, String comments,
            String memberRatingsSummary) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + reportId));

        if (report.getStatus() != Report.ReportStatus.PENDING_CHAIR_REVIEW
                && report.getStatus() != Report.ReportStatus.DRAFT) {
            throw new IllegalStateException(
                    "Report is not in PENDING_CHAIR_REVIEW or DRAFT status. Current status: " + report.getStatus());
        }

        User chair = userRepo.findById(chairId)
                .orElseThrow(() -> new IllegalArgumentException("Chair user not found with ID: " + chairId));

        // Validate the user has a Chair or Vice Chair role
        if (chair.getRole() != User.UserRole.CHAIR && chair.getRole() != User.UserRole.VICE_CHAIR) {
            throw new IllegalArgumentException("User does not have Chair privileges");
        }

        report.setReviewedByChair(chair);
        report.setChairComments(comments);
        report.setChairReviewedAt(java.time.LocalDateTime.now());

        if (memberRatingsSummary != null && !memberRatingsSummary.isBlank()) {
            report.setMemberRatingsSummary(memberRatingsSummary);
        }

        if (approved) {
            report.setStatus(Report.ReportStatus.SUBMITTED);
            report.setUpdatedAt(java.time.LocalDateTime.now());
            Report saved = reportRepo.save(report);

            // Notify HODs about the new report
            notifyHodAboutReport(saved);
            return saved;
        } else {
            report.setStatus(Report.ReportStatus.REJECTED_BY_CHAIR);
            report.setUpdatedAt(java.time.LocalDateTime.now());
            Report saved = reportRepo.save(report);

            // Notify the submitter about rejection
            if (report.getSubmittedBy() != null) {
                notificationService.createNotification(
                        report.getSubmittedBy().getId(),
                        "Report Returned by Chair",
                        "Your report for '" + report.getResolution().getTitle()
                                + "' has been returned by the Chair with feedback: " + comments,
                        Notification.NotificationType.REPORT_REJECTION,
                        "Report",
                        report.getId());
            }
            return saved;
        }
    }

    /**
     * Notify HODs in the same country about a newly submitted report.
     */
    private void notifyHodAboutReport(Report report) {
        try {
            List<User> hods = userRepo.findByRole(User.UserRole.HOD);
            List<User> chairs = userRepo.findByRole(User.UserRole.CHAIR);
            // Combined HOD + Chair (who may have HOD privileges)
            java.util.Set<Long> notified = new java.util.HashSet<>();
            for (User hod : hods) {
                if (notified.add(hod.getId())) {
                    notificationService.createNotification(
                            hod.getId(),
                            "New Report for Review",
                            "A new report from subcommittee '"
                                    + (report.getSubcommittee() != null ? report.getSubcommittee().getName()
                                            : "Unknown")
                                    + "' for resolution '" + report.getResolution().getTitle()
                                    + "' is ready for HOD review.",
                            Notification.NotificationType.REPORT_SUBMISSION,
                            "Report",
                            report.getId());
                }
            }
            // Also notify chairs with HOD privileges
            for (User chair : chairs) {
                if (hodPermissionService.hasHODPrivileges(chair) && notified.add(chair.getId())) {
                    notificationService.createNotification(
                            chair.getId(),
                            "New Report for Review",
                            "A new report from subcommittee '"
                                    + (report.getSubcommittee() != null ? report.getSubcommittee().getName()
                                            : "Unknown")
                                    + "' for resolution '" + report.getResolution().getTitle()
                                    + "' is ready for HOD review.",
                            Notification.NotificationType.REPORT_SUBMISSION,
                            "Report",
                            report.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Error notifying HODs about report: " + e.getMessage());
        }
    }

    // ── Report Attachment Workflow ───────────────────────────────────────────

    @Transactional
    public List<ReportAttachment> attachFilesToReport(Long reportId, List<MultipartFile> files, Long uploaderId) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files were provided.");
        }

        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + reportId));

        // Lock file changes once report is approved by HOD/Commissioner.
        if (report.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                || report.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER
                || report.getStatus() == Report.ReportStatus.REJECTED_BY_COMMISSIONER) {
            throw new IllegalStateException("Attachments are locked after HOD/Commissioner approval decisions.");
        }

        User uploader = null;
        if (uploaderId != null) {
            uploader = userRepo.findById(uploaderId).orElse(null);
        }
        final User finalUploader = uploader;

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> {
                    Document doc = documentService.storeFile(file);
                    ReportAttachment attachment = ReportAttachment.builder()
                            .report(report)
                            .document(doc)
                            .uploadedBy(finalUploader)
                            .uploadedAt(LocalDateTime.now())
                            .build();
                    return reportAttachmentRepo.save(attachment);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportAttachment> getReportAttachments(Long reportId) {
        return reportAttachmentRepo.findByReportIdOrderByUploadedAtDesc(reportId);
    }

    @Transactional(readOnly = true)
    public ReportAttachment getReportAttachment(Long reportId, Long attachmentId) {
        ReportAttachment attachment = reportAttachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found with ID: " + attachmentId));

        if (attachment.getReport() == null || !attachment.getReport().getId().equals(reportId)) {
            throw new IllegalArgumentException("Attachment does not belong to report " + reportId);
        }

        return attachment;
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentResource(Long reportId, Long attachmentId) {
        ReportAttachment attachment = getReportAttachment(reportId, attachmentId);
        Document doc = attachment.getDocument();
        if (doc == null) {
            throw new IllegalArgumentException("Attachment has no backing document.");
        }
        return documentService.loadFileAsResource(doc.getStoredFilename());
    }

    public Map<String, Object> toAttachmentInfo(ReportAttachment attachment) {
        Document doc = attachment.getDocument();
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("id", attachment.getId());
        info.put("reportId", attachment.getReport() != null ? attachment.getReport().getId() : null);
        info.put("filename", doc != null ? doc.getOriginalFilename() : "unknown");
        info.put("contentType", doc != null ? doc.getContentType() : null);
        info.put("size", doc != null ? doc.getFileSize() : null);
        info.put("uploadedAt", attachment.getUploadedAt());
        info.put("uploadedBy", attachment.getUploadedBy() != null ? attachment.getUploadedBy().getName() : null);
        return info;
    }

    private List<Report> sortReportsNewestFirst(List<Report> reports) {
        if (reports == null) {
            return List.of();
        }

        return reports.stream()
                .sorted(Comparator
                        .comparing(Report::getSubmittedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Report::getUpdatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Report::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
} 