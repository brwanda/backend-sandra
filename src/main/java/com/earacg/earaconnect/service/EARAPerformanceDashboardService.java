package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.*;
import com.earacg.earaconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for EARA Performance Dashboard — provides real database-driven metrics.
 */
@Service
@Slf4j
public class EARAPerformanceDashboardService {

    @Autowired private ReportRepo reportRepo;
    @Autowired private ResolutionRepo resolutionRepo;
    @Autowired private ResolutionAssignmentRepo assignmentRepo;
    @Autowired private SubTaskRepository subTaskRepo;
    @Autowired private MeetingRepo meetingRepo;
    @Autowired private AttendanceRepo attendanceRepo;
    @Autowired private CountryRepo countryRepo;
    @Autowired private SubCommitteeRepo subCommitteeRepo;
    @Autowired private UserRepo userRepo;

    // ── Performance Metrics KPIs ──────────────────────────────────────────

    public Map<String, Object> getPerformanceMetrics(String country, String committee, String timeFilter) {
        LocalDateTime since = getStartDate(timeFilter);
        LocalDateTime previousStart = getPreviousPeriodStart(timeFilter);

        List<Report> reports = getFilteredReports(country, committee, since);
        List<Report> previousReports = getFilteredReports(country, committee, previousStart, since);

        // Approval Rate: approved reports / total reviewed reports
        long totalReviewed = reports.stream().filter(r -> r.getStatus() != Report.ReportStatus.DRAFT
                && r.getStatus() != Report.ReportStatus.PENDING_CHAIR_REVIEW).count();
        long approved = reports.stream().filter(r ->
                r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                || r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER).count();
        double approvalRate = totalReviewed > 0 ? Math.round((double) approved / totalReviewed * 100.0) : 0;

        long prevReviewed = previousReports.stream().filter(r -> r.getStatus() != Report.ReportStatus.DRAFT
                && r.getStatus() != Report.ReportStatus.PENDING_CHAIR_REVIEW).count();
        long prevApproved = previousReports.stream().filter(r ->
                r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                || r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER).count();
        double prevApprovalRate = prevReviewed > 0 ? Math.round((double) prevApproved / prevReviewed * 100.0) : 0;

        // Task Completion: DONE tasks / total tasks
        List<SubTask> tasks = getFilteredTasks(country, committee, since);
        List<SubTask> prevTasks = getFilteredTasks(country, committee, previousStart, since);
        long doneTasks = tasks.stream().filter(t -> t.getStatus() == SubTask.TaskStatus.DONE).count();
        double taskCompletion = !tasks.isEmpty() ? Math.round((double) doneTasks / tasks.size() * 100.0) : 0;
        long prevDone = prevTasks.stream().filter(t -> t.getStatus() == SubTask.TaskStatus.DONE).count();
        double prevTaskCompletion = !prevTasks.isEmpty() ? Math.round((double) prevDone / prevTasks.size() * 100.0) : 0;

        // Avg Resolution Time (days): for completed resolutions filtered by country/committee
        List<Resolution> resolutions = resolutionRepo.findAll().stream()
                .filter(r -> r.getStatus() == Resolution.ResolutionStatus.COMPLETED && r.getUpdatedAt() != null && r.getCreatedAt() != null)
                .filter(r -> r.getCreatedAt().isAfter(since))
                .filter(r -> matchesResolutionFilters(r, country, committee))
                .toList();
        double avgResolutionTime = resolutions.stream()
                .mapToLong(r -> java.time.Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toDays())
                .average().orElse(0);
        avgResolutionTime = Math.round(avgResolutionTime);

        List<Resolution> prevResolutions = resolutionRepo.findAll().stream()
                .filter(r -> r.getStatus() == Resolution.ResolutionStatus.COMPLETED && r.getUpdatedAt() != null && r.getCreatedAt() != null)
                .filter(r -> r.getCreatedAt().isAfter(previousStart) && r.getCreatedAt().isBefore(since))
                .filter(r -> matchesResolutionFilters(r, country, committee))
                .toList();
        double prevAvgResTime = prevResolutions.stream()
                .mapToLong(r -> java.time.Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toDays())
                .average().orElse(0);

        // Member Participation: attendance present / total attendance records
        List<Attendance> attendances = getFilteredAttendance(country, committee, since);
        List<Attendance> prevAttendances = getFilteredAttendance(country, committee, previousStart, since);
        long present = attendances.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT
                || a.getStatus() == Attendance.AttendanceStatus.LATE).count();
        double participation = !attendances.isEmpty() ? Math.round((double) present / attendances.size() * 100.0) : 0;
        long prevPresent = prevAttendances.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT
                || a.getStatus() == Attendance.AttendanceStatus.LATE).count();
        double prevParticipation = !prevAttendances.isEmpty() ? Math.round((double) prevPresent / prevAttendances.size() * 100.0) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalRate", approvalRate);
        result.put("approvalRateChange", approvalRate - prevApprovalRate);
        result.put("reviewedCount", totalReviewed);
        result.put("approvedCount", approved);
        result.put("taskCompletion", taskCompletion);
        result.put("taskCompletionChange", taskCompletion - prevTaskCompletion);
        result.put("taskCount", tasks.size());
        result.put("doneTaskCount", doneTasks);
        result.put("averageResolutionTime", avgResolutionTime);
        result.put("resolutionTimeChange", Math.round(avgResolutionTime - prevAvgResTime));
        result.put("completedResolutionCount", resolutions.size());
        result.put("memberParticipation", participation);
        result.put("participationChange", participation - prevParticipation);
        result.put("attendanceCount", attendances.size());
        result.put("presentAttendanceCount", present);
        return result;
    }

    // ── Country Performance ───────────────────────────────────────────────

    public List<Map<String, Object>> getCountryPerformance(String country, String committee, String timeFilter) {
        LocalDateTime since = getStartDate(timeFilter);
        List<Country> countries = country.equals("all")
                ? countryRepo.findAll()
                : countryRepo.findAll().stream().filter(c -> c.getName().equals(country)).toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Country c : countries) {
            List<Report> reports = reportRepo.findAll().stream()
                    .filter(r -> r.getSubmittedBy() != null && r.getSubmittedBy().getCountry() != null
                            && r.getSubmittedBy().getCountry().getId().equals(c.getId()))
                    .filter(r -> r.getSubmittedAt() != null && r.getSubmittedAt().isAfter(since))
                    .filter(r -> matchesCommittee(r, committee))
                    .toList();

            long reviewed = reports.stream().filter(r -> r.getStatus() != Report.ReportStatus.DRAFT
                    && r.getStatus() != Report.ReportStatus.PENDING_CHAIR_REVIEW).count();
            long approved = reports.stream().filter(r ->
                    r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                    || r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER).count();
            double approvalRate = reviewed > 0 ? Math.round((double) approved / reviewed * 100.0) : 0;

            // Task completion for this country
            List<SubTask> countryTasks = subTaskRepo.findAll().stream()
                    .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().getCountry() != null
                            && t.getAssignedTo().getCountry().getId().equals(c.getId()))
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                    .toList();
            long done = countryTasks.stream().filter(t -> t.getStatus() == SubTask.TaskStatus.DONE).count();
            double taskCompletion = !countryTasks.isEmpty() ? Math.round((double) done / countryTasks.size() * 100.0) : 0;

            // Resolution time
            List<Resolution> completedRes = resolutionRepo.findAll().stream()
                    .filter(r -> r.getStatus() == Resolution.ResolutionStatus.COMPLETED)
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(since))
                    .filter(r -> r.getAssignments() != null && r.getAssignments().stream()
                            .anyMatch(a -> a.getSubcommittee() != null
                                    && userRepo.findBySubcommitteeId(a.getSubcommittee().getId()).stream()
                                    .anyMatch(u -> u.getCountry() != null && u.getCountry().getId().equals(c.getId()))))
                    .toList();
            double resTime = completedRes.stream()
                    .filter(r -> r.getUpdatedAt() != null)
                    .mapToLong(r -> java.time.Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toDays())
                    .average().orElse(0);

            // Participation
            List<Attendance> countryAtt = attendanceRepo.findAll().stream()
                    .filter(a -> a.getUser() != null && a.getUser().getCountry() != null
                            && a.getUser().getCountry().getId().equals(c.getId()))
                    .filter(a -> a.getMeeting() != null && a.getMeeting().getMeetingDate() != null
                            && a.getMeeting().getMeetingDate().isAfter(since))
                    .toList();
            long pres = countryAtt.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || a.getStatus() == Attendance.AttendanceStatus.LATE).count();
            double part = !countryAtt.isEmpty() ? Math.round((double) pres / countryAtt.size() * 100.0) : 0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("country", c.getName());
            row.put("approvalRate", approvalRate);
            row.put("taskCompletion", taskCompletion);
            row.put("resolutionTime", Math.round(resTime));
            row.put("participation", part);
            result.add(row);
        }
        return result;
    }

    // ── Resolution Status Distribution ────────────────────────────────────

    public List<Map<String, Object>> getResolutionStatus(String country, String committee, String timeFilter) {
        LocalDateTime since = getStartDate(timeFilter);
        List<Resolution> all = resolutionRepo.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(since))
                .toList();

        // Count by status
        long completed = all.stream().filter(r -> r.getStatus() == Resolution.ResolutionStatus.COMPLETED).count();
        long inProgress = all.stream().filter(r -> r.getStatus() == Resolution.ResolutionStatus.IN_PROGRESS).count();
        long assigned = all.stream().filter(r -> r.getStatus() == Resolution.ResolutionStatus.ASSIGNED).count();
        long cancelled = all.stream().filter(r -> r.getStatus() == Resolution.ResolutionStatus.CANCELLED).count();
        long total = all.size();

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("name", "Completed", "value", completed,
                "percentage", total > 0 ? Math.round((double) completed / total * 100) : 0, "color", "#10B981"));
        result.add(Map.of("name", "In Progress", "value", inProgress,
                "percentage", total > 0 ? Math.round((double) inProgress / total * 100) : 0, "color", "#F59E0B"));
        result.add(Map.of("name", "Assigned", "value", assigned,
                "percentage", total > 0 ? Math.round((double) assigned / total * 100) : 0, "color", "#3B82F6"));
        result.add(Map.of("name", "Cancelled", "value", cancelled,
                "percentage", total > 0 ? Math.round((double) cancelled / total * 100) : 0, "color", "#EF4444"));
        return result;
    }

    // ── Monthly Trends ────────────────────────────────────────────────────

    public List<Map<String, Object>> getMonthlyTrends(String country, String committee, String timeFilter) {
        int months = switch (timeFilter) {
            case "week" -> 1;
            case "month" -> 3;
            case "quarter" -> 6;
            case "year" -> 12;
            default -> 6;
        };

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM");
        YearMonth now = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

            // Reports approved in this month
            long approvals = reportRepo.findAll().stream()
                    .filter(r -> r.getSubmittedAt() != null && !r.getSubmittedAt().isBefore(start) && !r.getSubmittedAt().isAfter(end))
                    .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                            || r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER)
                    .count();

            // Tasks completed in this month
            long tasksCompleted = subTaskRepo.findAll().stream()
                    .filter(t -> t.getUpdatedAt() != null && !t.getUpdatedAt().isBefore(start) && !t.getUpdatedAt().isAfter(end))
                    .filter(t -> t.getStatus() == SubTask.TaskStatus.DONE)
                    .count();

            // Resolutions created in this month
            long resolutions = resolutionRepo.countByCreatedAtBetween(start, end);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.format(monthFmt));
            row.put("approvals", approvals);
            row.put("tasks", tasksCompleted);
            row.put("resolutions", resolutions);
            result.add(row);
        }
        return result;
    }

    // ── Task Assignments by Subcommittee ──────────────────────────────────

    public List<Map<String, Object>> getTaskAssignments(String country, String committee, String timeFilter) {
        LocalDateTime since = getStartDate(timeFilter);
        List<SubCommittee> subs = committee.equals("all")
                ? subCommitteeRepo.findAll()
                : subCommitteeRepo.findAll().stream().filter(s -> s.getName().equals(committee)).toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SubCommittee sub : subs) {
            List<SubTask> tasks = subTaskRepo.findAll().stream()
                    .filter(t -> t.getSubcommitteeId() != null && t.getSubcommitteeId().equals(sub.getId()))
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                    .toList();

            long total = tasks.size();
            long done = tasks.stream().filter(t -> t.getStatus() == SubTask.TaskStatus.DONE).count();
            long pending = tasks.stream().filter(t -> t.getStatus() == SubTask.TaskStatus.TODO
                    || t.getStatus() == SubTask.TaskStatus.IN_PROGRESS).count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("committee", sub.getName());
            row.put("assigned", total);
            row.put("completed", done);
            row.put("pending", pending);
            result.add(row);
        }
        return result;
    }

    // ── Gantt Data (Resolution timelines) ─────────────────────────────────

    public List<Map<String, Object>> getGanttData(String country, String committee, String timeFilter) {
        LocalDateTime since = getStartDate(timeFilter);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Resolution> resolutions = resolutionRepo.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(since))
                .sorted(Comparator.comparing(Resolution::getCreatedAt))
                .limit(10)
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Resolution r : resolutions) {
            String status = switch (r.getStatus()) {
                case COMPLETED -> "completed";
                case IN_PROGRESS -> "in-progress";
                case CANCELLED -> "cancelled";
                default -> "pending";
            };

            LocalDateTime endTime = r.getUpdatedAt() != null ? r.getUpdatedAt() : LocalDateTime.now();
            long days = java.time.Duration.between(r.getCreatedAt(), endTime).toDays();
            if (days < 1) days = 1;

            // Progress: completed = 100, in-progress = based on completed assignments, else 0
            int progress = 0;
            if (r.getStatus() == Resolution.ResolutionStatus.COMPLETED) {
                progress = 100;
            } else if (r.getAssignments() != null && !r.getAssignments().isEmpty()) {
                long completedAssignments = r.getAssignments().stream()
                        .filter(a -> a.getStatus() == ResolutionAssignment.AssignmentStatus.COMPLETED).count();
                progress = (int) Math.round((double) completedAssignments / r.getAssignments().size() * 100.0);
            }

            String title = r.getTitle();
            if (title != null && title.length() > 40) {
                title = title.substring(0, 37) + "...";
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("task", title != null ? title : "Resolution #" + r.getId());
            row.put("start", r.getCreatedAt().format(dateFmt));
            row.put("end", endTime.format(dateFmt));
            row.put("progress", progress);
            row.put("status", status);
            row.put("days", days);
            result.add(row);
        }
        return result;
    }

    // ── Helper Methods ────────────────────────────────────────────────────

    private LocalDateTime getStartDate(String timeFilter) {
        return switch (timeFilter) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "quarter" -> LocalDateTime.now().minusMonths(3);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusMonths(1);
        };
    }

    private LocalDateTime getPreviousPeriodStart(String timeFilter) {
        return switch (timeFilter) {
            case "week" -> LocalDateTime.now().minusWeeks(2);
            case "month" -> LocalDateTime.now().minusMonths(2);
            case "quarter" -> LocalDateTime.now().minusMonths(6);
            case "year" -> LocalDateTime.now().minusYears(2);
            default -> LocalDateTime.now().minusMonths(2);
        };
    }

    private List<Report> getFilteredReports(String country, String committee, LocalDateTime since) {
        return getFilteredReports(country, committee, since, null);
    }

    private List<Report> getFilteredReports(String country, String committee, LocalDateTime since, LocalDateTime until) {
        return reportRepo.findAll().stream()
                .filter(r -> r.getSubmittedAt() != null && r.getSubmittedAt().isAfter(since))
                .filter(r -> until == null || r.getSubmittedAt().isBefore(until))
                .filter(r -> country.equals("all") || (r.getSubmittedBy() != null && r.getSubmittedBy().getCountry() != null
                        && textMatches(r.getSubmittedBy().getCountry().getName(), country)))
                .filter(r -> matchesCommittee(r, committee))
                .toList();
    }

    private List<SubTask> getFilteredTasks(String country, String committee, LocalDateTime since) {
        return getFilteredTasks(country, committee, since, null);
    }

    private List<SubTask> getFilteredTasks(String country, String committee, LocalDateTime since, LocalDateTime until) {
        return subTaskRepo.findAll().stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .filter(t -> until == null || t.getCreatedAt().isBefore(until))
                .filter(t -> country.equals("all") || (t.getAssignedTo() != null && t.getAssignedTo().getCountry() != null
                        && textMatches(t.getAssignedTo().getCountry().getName(), country)))
                .filter(t -> committee.equals("all") || (t.getSubcommitteeId() != null
                        && subCommitteeRepo.findById(t.getSubcommitteeId())
                        .map(s -> textMatches(s.getName(), committee)).orElse(false)))
                .toList();
    }

    private List<Attendance> getFilteredAttendance(String country, String committee, LocalDateTime since) {
        return getFilteredAttendance(country, committee, since, null);
    }

    private List<Attendance> getFilteredAttendance(String country, String committee, LocalDateTime since, LocalDateTime until) {
        return attendanceRepo.findAll().stream()
                .filter(a -> a.getMeeting() != null && a.getMeeting().getMeetingDate() != null
                        && a.getMeeting().getMeetingDate().isAfter(since))
                .filter(a -> until == null || a.getMeeting().getMeetingDate().isBefore(until))
                .filter(a -> country.equals("all") || (a.getUser() != null && a.getUser().getCountry() != null
                        && textMatches(a.getUser().getCountry().getName(), country)))
                .filter(a -> committee.equals("all") || (a.getMeeting().getSubCommittee() != null
                        && textMatches(a.getMeeting().getSubCommittee().getName(), committee)))
                .toList();
    }

    private boolean matchesCommittee(Report r, String committee) {
        if (committee.equals("all")) return true;
        return r.getSubcommittee() != null && textMatches(r.getSubcommittee().getName(), committee);
    }

        private boolean matchesResolutionFilters(Resolution resolution, String country, String committee) {
                if (resolution == null) return false;
                if (resolution.getAssignments() == null || resolution.getAssignments().isEmpty()) return false;

                return resolution.getAssignments().stream().anyMatch(assignment -> {
                        SubCommittee subcommittee = assignment.getSubcommittee();
                        if (subcommittee == null) return false;

                        if (!"all".equals(committee) && !textMatches(subcommittee.getName(), committee)) {
                                return false;
                        }

                        if ("all".equals(country)) {
                                return true;
                        }

                        return userRepo.findBySubcommitteeId(subcommittee.getId()).stream()
                                        .anyMatch(user -> user.getCountry() != null && textMatches(user.getCountry().getName(), country));
                });
        }

        private boolean textMatches(String actual, String selected) {
                return normalizeText(actual).equals(normalizeText(selected));
        }

        private String normalizeText(String value) {
                if (value == null) return "";
                return value.trim()
                                .toLowerCase()
                                .replaceAll("[^a-z0-9]", "");
        }
}
