package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Report;
import com.earacg.earaconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private MeetingRepo meetingRepo;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private MeetingInvitationRepo meetingInvitationRepo;

    @Autowired
    private ResolutionAssignmentRepo resolutionAssignmentRepo;

    @Autowired
    private ReportRepo reportRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CSubCommitteeMembersRepo cSubCommitteeMembersRepo;

    public Map<String, Object> getPerformanceDashboardData(String timeFilter, String subcommitteeFilter, Long userId) {
        Map<String, Object> dashboardData = new HashMap<>();

        try {
            // Get real data from database
            List<Report> allReports = reportRepo.findAll();

            // Calculate summary statistics
            Map<String, Object> summary = new HashMap<>();
            long totalReports = allReports.size();
            long approvedReports = allReports.stream().filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD)
                    .count();
            long rejectedReports = allReports.stream().filter(r -> r.getStatus() == Report.ReportStatus.REJECTED_BY_HOD)
                    .count();
            long pendingReports = allReports.stream().filter(r -> r.getStatus() == Report.ReportStatus.SUBMITTED)
                    .count();

                double averagePerformance = allReports.stream()
                    .mapToDouble(this::resolveEffectiveReportPerformance)
                    .average().orElse(0.0);

            long totalSubcommittees = subCommitteeRepo.count();
            long activeResolutions = resolutionRepo.count();

            summary.put("totalReports", totalReports);
            summary.put("approvedReports", approvedReports);
            summary.put("rejectedReports", rejectedReports);
            summary.put("pendingReports", pendingReports);
            summary.put("averagePerformance", Math.round(averagePerformance * 100.0) / 100.0);
            summary.put("totalSubcommittees", totalSubcommittees);
            summary.put("activeResolutions", activeResolutions);

            dashboardData.put("summary", summary);
            dashboardData.put("subcommitteePerformance", getRealSubcommitteePerformance(allReports));
            dashboardData.put("monthlyTrend", getRealMonthlyTrend());
            dashboardData.put("resolutionProgress", getRealResolutionProgress(allReports));
            dashboardData.put("performanceDistribution", getRealPerformanceDistribution(allReports));

        } catch (Exception e) {
            System.err.println("Error getting dashboard data: " + e.getMessage());
            dashboardData.put("error", e.getMessage());
            // Return empty structure instead of mock data
            dashboardData.put("summary", new HashMap<>());
            dashboardData.put("subcommitteePerformance", new ArrayList<>());
            dashboardData.put("monthlyTrend", new HashMap<>());
            dashboardData.put("resolutionProgress", new ArrayList<>());
            dashboardData.put("performanceDistribution", new HashMap<>());
        }

        return dashboardData;
    }

    public Map<String, Object> getPerformanceStats(Long hodId, Long commissionerId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<Report> allReports = reportRepo.findAll();

            long pendingReports;
            if (commissionerId != null) {
                pendingReports = allReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD)
                        .count();
            } else if (hodId != null) {
                pendingReports = allReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.SUBMITTED)
                        .count();
            } else {
                pendingReports = allReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.SUBMITTED)
                        .count();
            }

            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

            long approvedThisMonth = allReports.stream()
                    .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                            || r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER)
                    .filter(r -> (r.getHodReviewedAt() != null && r.getHodReviewedAt().isAfter(startOfMonth)) ||
                            (r.getCommissionerReviewedAt() != null
                                    && r.getCommissionerReviewedAt().isAfter(startOfMonth)))
                    .count();

            long rejectedThisMonth = allReports.stream()
                    .filter(r -> r.getStatus() == Report.ReportStatus.REJECTED_BY_HOD
                            || r.getStatus() == Report.ReportStatus.REJECTED_BY_COMMISSIONER)
                    .filter(r -> (r.getHodReviewedAt() != null && r.getHodReviewedAt().isAfter(startOfMonth)) ||
                            (r.getCommissionerReviewedAt() != null
                                    && r.getCommissionerReviewedAt().isAfter(startOfMonth)))
                    .count();

                double averagePerformance = allReports.stream()
                    .mapToDouble(this::resolveEffectiveReportPerformance)
                    .average().orElse(0.0);

            stats.put("pendingReports", pendingReports);
            stats.put("approvedThisMonth", approvedThisMonth);
            stats.put("rejectedThisMonth", rejectedThisMonth);
            stats.put("averagePerformance", (int) Math.round(averagePerformance));
            stats.put("activeResolutions", resolutionRepo.count());
            stats.put("totalSubcommittees", subCommitteeRepo.count());
            stats.put("subcommitteePerformance", getRealSubcommitteePerformance(allReports));
            stats.put("monthlyTrend", getRealMonthlyTrend());

        } catch (Exception e) {
            System.err.println("Error calculating performance stats: " + e.getMessage());
            stats.put("pendingReports", 0);
            stats.put("approvedThisMonth", 0);
            stats.put("rejectedThisMonth", 0);
            stats.put("averagePerformance", 0);
        }

        return stats;
    }

    public Map<String, Object> getSubcommitteePerformanceData(String timeFilter) {
        Map<String, Object> data = new HashMap<>();
        data.put("subcommitteePerformance", getRealSubcommitteePerformance());
        return data;
    }

    public Map<String, Object> getResolutionProgressData() {
        Map<String, Object> data = new HashMap<>();
        data.put("resolutionProgress", getRealResolutionProgress());
        return data;
    }

    public Map<String, Object> getMonthlyTrendsData(Integer months) {
        Map<String, Object> data = new HashMap<>();
        data.put("monthlyTrend", getRealMonthlyTrend());
        return data;
    }

    public Map<String, Object> getComprehensiveDashboardData(Long userId, String userRole) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<Report> allReports = reportRepo.findAll();

            long totalReports = allReports.size();
            long approvedReports = allReports.stream()
                    .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER)
                    .count();
            long rejectedReports = allReports.stream()
                    .filter(r -> r.getStatus() == Report.ReportStatus.REJECTED_BY_COMMISSIONER)
                    .count();

            long pendingReviewCount;
            if ("COMMISSIONER_GENERAL".equals(userRole)) {
                pendingReviewCount = allReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD)
                        .count();
            } else {
                pendingReviewCount = allReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.SUBMITTED)
                        .count();
            }

                double averagePerformance = allReports.stream()
                    .mapToDouble(this::resolveEffectiveReportPerformance)
                    .average().orElse(0.0);

            long activeResolutions = resolutionRepo.count();

            // Group pre-loaded reports by resolution to avoid N+1 queries
            Map<Long, List<Report>> reportsByResolution = allReports.stream()
                    .filter(r -> r.getResolution() != null)
                    .collect(Collectors.groupingBy(r -> r.getResolution().getId()));

            long completedResolutions = reportsByResolution.values().stream()
                    .filter(resReports -> {
                        if (resReports.isEmpty())
                            return false;
                        double avg = resReports.stream()
                            .mapToDouble(this::resolveEffectiveReportPerformance)
                            .average().orElse(0.0);
                        return avg >= 95.0; // Threshold for completion
                    }).count();

            stats.put("totalReports", totalReports);
            stats.put("averagePerformance", (int) Math.round(averagePerformance));
            stats.put("pendingReview", pendingReviewCount);
            stats.put("approvedReports", approvedReports);
            stats.put("rejectedReports", rejectedReports);
            stats.put("completedResolutions", completedResolutions);
            stats.put("activeResolutions", activeResolutions);

            // Status counts
            Map<String, Long> statusCounts = allReports.stream()
                    .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));
            stats.put("statusCounts", statusCounts);

            // Subcommittee performance (reuse pre-loaded allReports)
            stats.put("subcommitteePerformance", getRealSubcommitteePerformance(allReports));

            // Monthly trends
            stats.put("monthlyTrends", getRealMonthlyTrendList());

            // Resolution progress (reuse pre-loaded allReports)
            stats.put("resolutionProgress", getRealResolutionProgress(allReports));

        } catch (Exception e) {
            System.err.println("Error calculating comprehensive dashboard data: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    private List<Map<String, Object>> getRealMonthlyTrendList() {
        List<Map<String, Object>> trends = new ArrayList<>();
        try {
            Map<String, Object> trendData = getRealMonthlyTrend();
            if (trendData.isEmpty())
                return trends;

            List<String> labels = (List<String>) trendData.get("labels");
            List<Integer> approved = (List<Integer>) trendData.get("approved");
            List<Integer> rejected = (List<Integer>) trendData.get("rejected");
            List<Integer> pending = (List<Integer>) trendData.get("pending");

            for (int i = 0; i < labels.size(); i++) {
                Map<String, Object> month = new HashMap<>();
                month.put("month", labels.get(i));
                month.put("approved", approved.get(i));
                month.put("rejected", rejected.get(i));
                month.put("pending", pending != null && i < pending.size() ? pending.get(i) : 0);
                trends.add(month);
            }
        } catch (Exception e) {
            System.err.println("Error in getRealMonthlyTrendList: " + e.getMessage());
        }
        return trends;
    }

    private List<Map<String, Object>> getSubcommitteePerformance() {
        // Redirecting to get real data instead of mock list
        return getRealSubcommitteePerformance();
    }

    private Map<String, Object> getMonthlyTrend() {
        // Redirecting to get real data
        return getRealMonthlyTrend();
    }

    private List<Map<String, Object>> getResolutionProgress() {
        // Redirecting to get real data
        return getRealResolutionProgress();
    }

    private Map<String, Object> getPerformanceDistribution() {
        // Redirecting to get real data
        return getRealPerformanceDistribution();
    }

    // Real data methods using database queries
    private List<Map<String, Object>> getRealSubcommitteePerformance() {
        return getRealSubcommitteePerformance(reportRepo.findAll());
    }

    private List<Map<String, Object>> getRealSubcommitteePerformance(List<Report> reports) {
        List<Map<String, Object>> performance = new ArrayList<>();

        try {
            Map<String, List<Double>> subcommitteePerformances = new HashMap<>();
            Map<String, String> subcommitteeNames = new HashMap<>();

            for (Report report : reports) {
                if (report.getSubcommittee() != null) {
                    String subcommitteeId = report.getSubcommittee().getId().toString();
                    String subcommitteeName = report.getSubcommittee().getName();
                    double effectivePerformance = resolveEffectiveReportPerformance(report);

                    subcommitteePerformances.computeIfAbsent(subcommitteeId, key -> new ArrayList<>())
                            .add(effectivePerformance);
                    subcommitteeNames.put(subcommitteeId, subcommitteeName);
                }
            }

            for (Map.Entry<String, List<Double>> entry : subcommitteePerformances.entrySet()) {
                String subcommitteeId = entry.getKey();
                List<Double> performances = entry.getValue();

                double average = performances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("name", subcommitteeNames.get(subcommitteeId));
                dataPoint.put("avgPerformance", (int) Math.round(average));
                dataPoint.put("reportCount", performances.size());
                dataPoint.put("totalReports", performances.size());
                // Calculate completion rate from subtasks for this subcommittee
                long scId = Long.parseLong(subcommitteeId);
                long totalSubTasks = subTaskRepository.findBySubcommitteeId(scId).size();
                long doneSubTasks = subTaskRepository.findBySubcommitteeIdAndStatus(scId, com.earacg.earaconnect.model.SubTask.TaskStatus.DONE).size();
                int completionRate = totalSubTasks > 0 ? (int) Math.round((double) doneSubTasks / totalSubTasks * 100) : 0;
                dataPoint.put("completionRate", completionRate);
                dataPoint.put("trend", average >= 80 ? "up" : average >= 60 ? "stable" : "down");
                performance.add(dataPoint);
            }
        } catch (Exception e) {
            System.err.println("Error getting real subcommittee performance: " + e.getMessage());
        }

        return performance;
    }

    private Map<String, Object> getRealMonthlyTrend() {
        Map<String, Object> trend = new HashMap<>();

        try {
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<Report> reports = reportRepo.findBySubmittedAtAfter(sixMonthsAgo);

            Map<String, List<Report>> monthlyReports = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");

            for (Report report : reports) {
                if (report.getSubmittedAt() != null) {
                    String monthKey = report.getSubmittedAt().format(formatter);
                    monthlyReports.computeIfAbsent(monthKey, key -> new ArrayList<>()).add(report);
                }
            }

            List<String> labels = new ArrayList<>();
            List<Integer> approved = new ArrayList<>();
            List<Integer> rejected = new ArrayList<>();
            List<Integer> avgPerformance = new ArrayList<>();

            for (int i = 5; i >= 0; i--) {
                LocalDateTime month = LocalDateTime.now().minusMonths(i);
                String monthLabel = month.format(formatter);
                labels.add(monthLabel);

                List<Report> monthReports = monthlyReports.getOrDefault(monthLabel, new ArrayList<>());

                long approvedCount = monthReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD).count();
                long rejectedCount = monthReports.stream()
                        .filter(r -> r.getStatus() == Report.ReportStatus.REJECTED_BY_HOD).count();
                double avgPerf = monthReports.stream()
                    .mapToDouble(this::resolveEffectiveReportPerformance)
                    .average().orElse(0.0);

                approved.add((int) approvedCount);
                rejected.add((int) rejectedCount);
                avgPerformance.add((int) Math.round(avgPerf));
            }

            trend.put("labels", labels);
            trend.put("approved", approved);
            trend.put("rejected", rejected);
            trend.put("avgPerformance", avgPerformance);

        } catch (Exception e) {
            System.err.println("Error getting real monthly trend: " + e.getMessage());
        }

        return trend;
    }

    private List<Map<String, Object>> getRealResolutionProgress() {
        return getRealResolutionProgress(reportRepo.findAll());
    }

    private List<Map<String, Object>> getRealResolutionProgress(List<Report> reports) {
        List<Map<String, Object>> progress = new ArrayList<>();

        try {
            Map<String, List<Report>> resolutionReports = new HashMap<>();

            for (Report report : reports) {
                if (report.getResolution() != null) {
                    String resolutionTitle = report.getResolution().getTitle();
                    resolutionReports.computeIfAbsent(resolutionTitle, key -> new ArrayList<>()).add(report);
                }
            }

            for (Map.Entry<String, List<Report>> entry : resolutionReports.entrySet()) {
                String resolutionTitle = entry.getKey();
                List<Report> resolutionReportsList = entry.getValue();

                double avgPerformance = resolutionReportsList.stream()
                    .mapToDouble(this::resolveEffectiveReportPerformance)
                    .average().orElse(0.0);

                Set<String> uniqueSubcommittees = new HashSet<>();
                for (Report report : resolutionReportsList) {
                    if (report.getSubcommittee() != null) {
                        uniqueSubcommittees.add(report.getSubcommittee().getName());
                    }
                }

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("resolution", resolutionTitle);
                dataPoint.put("progress", (int) Math.round(avgPerformance));
                dataPoint.put("subcommittees", uniqueSubcommittees.size());

                progress.add(dataPoint);
            }
        } catch (Exception e) {
            System.err.println("Error getting real resolution progress: " + e.getMessage());
        }

        return progress;
    }

    private Map<String, Object> getRealPerformanceDistribution() {
        return getRealPerformanceDistribution(reportRepo.findAll());
    }

    private Map<String, Object> getRealPerformanceDistribution(List<Report> reports) {
        Map<String, Object> distribution = new HashMap<>();

        try {

            int excellent = 0, veryGood = 0, good = 0, satisfactory = 0, poor = 0;

            for (Report report : reports) {
                int perf = (int) Math.round(resolveEffectiveReportPerformance(report));
                if (perf >= 90)
                    excellent++;
                else if (perf >= 80)
                    veryGood++;
                else if (perf >= 70)
                    good++;
                else if (perf >= 60)
                    satisfactory++;
                else
                    poor++;
            }

            distribution.put("excellent", excellent);
            distribution.put("veryGood", veryGood);
            distribution.put("good", good);
            distribution.put("satisfactory", satisfactory);
            distribution.put("poor", poor);

        } catch (Exception e) {
            System.err.println("Error getting real performance distribution: " + e.getMessage());
        }

        return distribution;
    }

    public List<Integer> getAvailableYears() {
        List<Integer> years = new ArrayList<>();
        try {
            // Get years from reports table
            List<Report> reports = reportRepo.findAll();
            Set<Integer> uniqueYears = new HashSet<>();

            for (Report report : reports) {
                if (report.getSubmittedAt() != null) {
                    uniqueYears.add(report.getSubmittedAt().getYear());
                }
            }

            years = new ArrayList<>(uniqueYears);
            years.sort(Collections.reverseOrder());

            if (years.isEmpty()) {
                years = Arrays.asList(java.time.Year.now().getValue());
            }

        } catch (Exception e) {
            System.err.println("Error getting available years: " + e.getMessage());
            years = Arrays.asList(java.time.Year.now().getValue());
        }

        return years;
    }

    public Map<String, Object> getSimplePerformanceData(Integer year) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Pre-load data once to avoid redundant DB queries
            List<Report> allReports = reportRepo.findAll();
            List<com.earacg.earaconnect.model.SubCommittee> allSubcommittees = subCommitteeRepo.findAll();

            List<Map<String, Object>> subcommittees = getSubcommitteePerformanceDataByYear(year, allReports, allSubcommittees);
            Map<String, Object> monthlyOverview = getMonthlyOverviewData(year, allReports);

            response.put("subcommittees", subcommittees);
            response.put("monthlyOverview", monthlyOverview);

        } catch (Exception e) {
            System.err.println("Error in getSimplePerformanceData: " + e.getMessage());
            response.put("subcommittees", new ArrayList<>());
            response.put("monthlyOverview", new HashMap<>());
        }

        return response;
    }

    private List<Map<String, Object>> getCountryPerformanceData(Integer year) {
        List<Map<String, Object>> countries = new ArrayList<>();

        try {
            // Get all countries from the database
            // For now, we'll use sample data based on existing reports
            List<Report> reports = reportRepo.findAll();
            Map<String, List<Report>> countryReports = new HashMap<>();

            // Group reports by country (through user)
            for (Report report : reports) {
                if (report.getSubmittedBy() != null &&
                        report.getSubmittedBy().getCountry() != null) {
                    String countryName = report.getSubmittedBy().getCountry().getName();
                    countryReports.computeIfAbsent(countryName, k -> new ArrayList<>()).add(report);
                }
            }

            // Calculate performance for each country
            for (Map.Entry<String, List<Report>> entry : countryReports.entrySet()) {
                String countryName = entry.getKey();
                List<Report> countryReportList = entry.getValue();

                // Filter reports for the specified year
                List<Report> yearReports = countryReportList.stream()
                        .filter(r -> r.getSubmittedAt() != null && r.getSubmittedAt().getYear() == year)
                        .collect(Collectors.toList());

                if (!yearReports.isEmpty()) {
                    Map<String, Object> countryData = new HashMap<>();
                    countryData.put("name", countryName);
                    countryData.put("reports", yearReports.size());

                    // Calculate approval rate
                    long approvedCount = yearReports.stream()
                            .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD)
                            .count();
                    double approvalRate = (double) approvedCount / yearReports.size() * 100;
                    countryData.put("approvalRate", Math.round(approvalRate * 100.0) / 100.0);

                    // Trend bands: up >=80, stable 60-79, down <60.
                    String trend = approvalRate >= 80 ? "up" : (approvalRate >= 60 ? "stable" : "down");
                    countryData.put("trend", trend);

                    // For now, set assigned resolutions to a sample value
                    // This would need to be calculated from resolution_assignments table
                    countryData.put("assignedResolutions", Math.max(1, yearReports.size() / 2));

                    countries.add(countryData);
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting country performance data: " + e.getMessage());
        }

        // Always return real data — no sample data fallback
        return countries;
    }

    private List<Map<String, Object>> getSubcommitteePerformanceDataByYear(Integer year) {
        return getSubcommitteePerformanceDataByYear(year, reportRepo.findAll(), subCommitteeRepo.findAll());
    }

    private List<Map<String, Object>> getSubcommitteePerformanceDataByYear(Integer year, List<Report> reports, List<com.earacg.earaconnect.model.SubCommittee> allSubcommittees) {
        List<Map<String, Object>> subcommittees = new ArrayList<>();

        try {

            System.out.println("🔍 DashboardService: Found " + reports.size() + " total reports");
            System.out.println("🔍 DashboardService: Looking for year: " + year);

            // Debug: Check what years are available in the reports
            Set<Integer> availableYears = reports.stream()
                    .filter(r -> r.getSubmittedAt() != null)
                    .map(r -> r.getSubmittedAt().getYear())
                    .collect(Collectors.toSet());
            System.out.println("🔍 DashboardService: Available years in reports: " + availableYears);

            // Group reports by subcommittee
            Map<String, List<Report>> subcommitteeReports = new HashMap<>();

            for (Report report : reports) {
                if (report.getSubcommittee() != null) {
                    String subcommitteeName = report.getSubcommittee().getName();
                    subcommitteeReports.computeIfAbsent(subcommitteeName, k -> new ArrayList<>()).add(report);
                }
            }

            System.out.println(
                    "🔍 DashboardService: Found " + subcommitteeReports.size() + " subcommittees with reports");

            // Calculate performance for each subcommittee
            for (com.earacg.earaconnect.model.SubCommittee subcommittee : allSubcommittees) {
                String subcommitteeName = subcommittee.getName();
                List<Report> subcommitteeReportList = subcommitteeReports.getOrDefault(subcommitteeName,
                        new ArrayList<>());

                System.out.println("🔍 DashboardService: Subcommittee " + subcommitteeName + " has "
                        + subcommitteeReportList.size() + " reports");

                // Filter reports for the specified year - but be more flexible
                List<Report> yearReports = subcommitteeReportList.stream()
                        .filter(r -> r.getSubmittedAt() != null && r.getSubmittedAt().getYear() == year)
                        .collect(Collectors.toList());

                // If no reports for the specific year, use all reports for this subcommittee
                if (yearReports.isEmpty() && !subcommitteeReportList.isEmpty()) {
                    System.out.println("🔍 DashboardService: No reports for year " + year + ", using all reports for "
                            + subcommitteeName);
                    yearReports = subcommitteeReportList;
                }

                if (!yearReports.isEmpty()) {
                    Map<String, Object> subcommitteeData = new HashMap<>();
                    subcommitteeData.put("name", subcommitteeName);
                    subcommitteeData.put("reports", yearReports.size());

                    // Calculate approval rate
                    long approvedCount = yearReports.stream()
                            .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                                    || r.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER)
                            .count();
                    double approvalRate = (double) approvedCount / yearReports.size() * 100;
                    subcommitteeData.put("approvalRate", Math.round(approvalRate * 100.0) / 100.0);

                        // Calculate effective performance with fallback sources when stored
                        // report.performancePercentage is zero/missing.
                        double avgPerformance = yearReports.stream()
                            .mapToDouble(this::resolveEffectiveReportPerformance)
                            .average()
                            .orElse(0.0);

                        // If effective report performance is still zero but we have approvals,
                        // use approval rate as a conservative fallback signal.
                        if (avgPerformance <= 0.0 && approvalRate > 0.0) {
                        avgPerformance = approvalRate;
                        }
                    subcommitteeData.put("performancePercentage", Math.round(avgPerformance * 100.0) / 100.0);

                    // Calculate task assignment percentage from actual resolution assignments
                    long assignedResolutions = 0;
                    try {
                        assignedResolutions = resolutionAssignmentRepo.countBySubcommitteeId(subcommittee.getId());
                    } catch (Exception ex) {
                        assignedResolutions = yearReports.size();
                    }
                    double taskAssignmentPercentage = assignedResolutions > 0
                            ? Math.min(100.0, (double) assignedResolutions / Math.max(1, yearReports.size()) * 100)
                            : 0;
                    subcommitteeData.put("taskAssignmentPercentage",
                            Math.round(taskAssignmentPercentage * 100.0) / 100.0);

                    // Trend bands: up >=80, stable 60-79, down <60.
                    String trend = avgPerformance >= 80 ? "up" : (avgPerformance >= 60 ? "stable" : "down");
                    subcommitteeData.put("trend", trend);

                    subcommitteeData.put("assignedResolutions", assignedResolutions);

                    // Also provide totalReports, avgPerformance and completionRate for frontend compatibility
                    subcommitteeData.put("totalReports", yearReports.size());
                    subcommitteeData.put("avgPerformance", (int) Math.round(avgPerformance));
                    // Calculate completion rate from subtasks
                    long scTotalTasks = subTaskRepository.findBySubcommitteeId(subcommittee.getId()).size();
                    long scDoneTasks = subTaskRepository.findBySubcommitteeIdAndStatus(subcommittee.getId(), com.earacg.earaconnect.model.SubTask.TaskStatus.DONE).size();
                    int scCompletionRate = scTotalTasks > 0 ? (int) Math.round((double) scDoneTasks / scTotalTasks * 100) : 0;
                    subcommitteeData.put("completionRate", scCompletionRate);

                    subcommittees.add(subcommitteeData);
                    System.out.println("✅ DashboardService: Added data for " + subcommitteeName + " with "
                            + yearReports.size() + " reports, avgPerf=" + avgPerformance);
                } else {
                    // Even without reports, show subcommittee with 0% performance
                    Map<String, Object> subcommitteeData = new HashMap<>();
                    subcommitteeData.put("name", subcommitteeName);
                    subcommitteeData.put("reports", 0);
                    subcommitteeData.put("approvalRate", 0.0);
                    subcommitteeData.put("performancePercentage", 0.0);
                    subcommitteeData.put("taskAssignmentPercentage", 0.0);
                    subcommitteeData.put("trend", "stable");
                    long assignedRes = 0;
                    try {
                        assignedRes = resolutionAssignmentRepo.countBySubcommitteeId(subcommittee.getId());
                    } catch (Exception ignored) {}
                    subcommitteeData.put("assignedResolutions", assignedRes);
                    subcommittees.add(subcommitteeData);
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting subcommittee performance data: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("🔍 DashboardService: Returning " + subcommittees.size() + " subcommittees");

        // Always return real data — no sample data fallback
        return subcommittees;
    }

    /**
     * Resolve report performance with fallback sources so older/bad zero values do not
     * flatten dashboard charts.
     */
    private double resolveEffectiveReportPerformance(Report report) {
        if (report == null) {
            return 0.0;
        }

        if (report.getPerformancePercentage() != null && report.getPerformancePercentage() > 0) {
            return report.getPerformancePercentage();
        }

        if (report.getHodRanking() != null && report.getHodRanking() > 0) {
            return Math.min(100.0, report.getHodRanking() * 20.0);
        }

        if (report.getResolution() != null && report.getResolution().getId() != null
                && report.getSubcommittee() != null && report.getSubcommittee().getId() != null) {
            try {
                List<com.earacg.earaconnect.model.SubTask> tasks = subTaskRepository
                        .findByResolutionIdAndSubcommitteeId(report.getResolution().getId(), report.getSubcommittee().getId());
                if (!tasks.isEmpty()) {
                    long done = tasks.stream()
                            .filter(t -> t.getStatus() == com.earacg.earaconnect.model.SubTask.TaskStatus.DONE)
                            .count();
                    return Math.round(((double) done / tasks.size()) * 10000.0) / 100.0;
                }
            } catch (Exception ignored) {
                // Keep fallback chain resilient; return 0 below.
            }
        }

        if (report.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                || report.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER) {
            // Approved reports should not collapse to 0 when historical numeric fields are missing.
            return 60.0;
        }

        return 0.0;
    }

    private Map<String, Object> getMonthlyOverviewData(Integer year) {
        return getMonthlyOverviewData(year, reportRepo.findAll());
    }

    private Map<String, Object> getMonthlyOverviewData(Integer year, List<Report> reports) {
        Map<String, Object> overview = new HashMap<>();

        try {
            System.out.println(
                    "🔍 DashboardService: getMonthlyOverviewData - Found " + reports.size() + " total reports");
            System.out.println("🔍 DashboardService: getMonthlyOverviewData - Looking for year: " + year);

            // Filter reports for the specified year
            List<Report> yearReports = reports.stream()
                    .filter(r -> r.getSubmittedAt() != null && r.getSubmittedAt().getYear() == year)
                    .collect(Collectors.toList());

            System.out.println("🔍 DashboardService: getMonthlyOverviewData - Found " + yearReports.size()
                    + " reports for year " + year);

            // If no reports for the specific year, use all reports
            if (yearReports.isEmpty() && !reports.isEmpty()) {
                System.out.println("🔍 DashboardService: getMonthlyOverviewData - No reports for year " + year
                        + ", using all reports");
                yearReports = reports;
            }

            long totalReports = yearReports.size();
            long approvedReports = yearReports.stream()
                    .filter(r -> r.getStatus() == Report.ReportStatus.APPROVED_BY_HOD)
                    .count();
            long totalReviews = yearReports.stream()
                    .filter(r -> r.getStatus() != Report.ReportStatus.SUBMITTED)
                    .count();

            double approvalRate = totalReports > 0 ? (double) approvedReports / totalReports * 100 : 0;

            System.out.println("🔍 DashboardService: getMonthlyOverviewData - Total reports: " + totalReports +
                    ", Approved: " + approvedReports + ", Reviews: " + totalReviews +
                    ", Approval rate: " + approvalRate + "%");

            overview.put("approvalRate", Math.round(approvalRate * 100.0) / 100.0);
            overview.put("totalReviews", totalReviews);
            overview.put("totalReports", totalReports);
            overview.put("totalResolutions", Math.max(1, totalReports / 2)); // Sample value for now

        } catch (Exception e) {
            System.err.println("Error getting monthly overview data: " + e.getMessage());
            e.printStackTrace();
            overview.put("approvalRate", 0.0);
            overview.put("totalReviews", 0L);
            overview.put("totalReports", 0L);
            overview.put("totalResolutions", 0L);
        }

        return overview;
    }

    private List<Map<String, Object>> getSampleCountryData() {
        List<Map<String, Object>> sampleData = new ArrayList<>();

        Map<String, Object> kenya = new HashMap<>();
        kenya.put("name", "Kenya");
        kenya.put("reports", 45);
        kenya.put("assignedResolutions", 38);
        kenya.put("approvalRate", 89.5);
        kenya.put("trend", "up");
        sampleData.add(kenya);

        Map<String, Object> uganda = new HashMap<>();
        uganda.put("name", "Uganda");
        uganda.put("reports", 32);
        uganda.put("assignedResolutions", 28);
        uganda.put("approvalRate", 78.2);
        uganda.put("trend", "stable");
        sampleData.add(uganda);

        Map<String, Object> tanzania = new HashMap<>();
        tanzania.put("name", "Tanzania");
        tanzania.put("reports", 28);
        tanzania.put("assignedResolutions", 25);
        tanzania.put("approvalRate", 85.7);
        tanzania.put("trend", "up");
        sampleData.add(tanzania);

        return sampleData;
    }

    private List<Map<String, Object>> getSampleSubcommitteeData() {
        List<Map<String, Object>> sampleData = new ArrayList<>();

        Map<String, Object> domesticRevenue = new HashMap<>();
        domesticRevenue.put("name", "Domestic Revenue Sub Committee");
        domesticRevenue.put("reports", 45);
        domesticRevenue.put("assignedResolutions", 38);
        domesticRevenue.put("approvalRate", 89.5);
        domesticRevenue.put("performancePercentage", 87.2);
        domesticRevenue.put("taskAssignmentPercentage", 84.4);
        domesticRevenue.put("trend", "up");
        sampleData.add(domesticRevenue);

        Map<String, Object> customsRevenue = new HashMap<>();
        customsRevenue.put("name", "Customs Revenue Sub Committee");
        customsRevenue.put("reports", 32);
        customsRevenue.put("assignedResolutions", 28);
        customsRevenue.put("approvalRate", 78.2);
        customsRevenue.put("performancePercentage", 79.5);
        customsRevenue.put("taskAssignmentPercentage", 87.5);
        customsRevenue.put("trend", "stable");
        sampleData.add(customsRevenue);

        Map<String, Object> itCommittee = new HashMap<>();
        itCommittee.put("name", "IT Sub Committee");
        itCommittee.put("reports", 28);
        itCommittee.put("assignedResolutions", 25);
        itCommittee.put("approvalRate", 85.7);
        itCommittee.put("performancePercentage", 91.3);
        itCommittee.put("taskAssignmentPercentage", 89.3);
        itCommittee.put("trend", "up");
        sampleData.add(itCommittee);

        Map<String, Object> legalCommittee = new HashMap<>();
        legalCommittee.put("name", "Legal Sub Committee");
        legalCommittee.put("reports", 22);
        legalCommittee.put("assignedResolutions", 20);
        legalCommittee.put("approvalRate", 91.0);
        legalCommittee.put("performancePercentage", 88.7);
        legalCommittee.put("taskAssignmentPercentage", 90.9);
        legalCommittee.put("trend", "up");
        sampleData.add(legalCommittee);

        Map<String, Object> hrCommittee = new HashMap<>();
        hrCommittee.put("name", "HR Sub Committee");
        hrCommittee.put("reports", 18);
        hrCommittee.put("assignedResolutions", 16);
        hrCommittee.put("approvalRate", 72.5);
        hrCommittee.put("performancePercentage", 68.9);
        hrCommittee.put("taskAssignmentPercentage", 88.9);
        hrCommittee.put("trend", "down");
        sampleData.add(hrCommittee);

        Map<String, Object> researchCommittee = new HashMap<>();
        researchCommittee.put("name", "Research Sub Committee");
        researchCommittee.put("reports", 15);
        researchCommittee.put("assignedResolutions", 12);
        researchCommittee.put("approvalRate", 80.0);
        researchCommittee.put("performancePercentage", 82.1);
        researchCommittee.put("taskAssignmentPercentage", 80.0);
        researchCommittee.put("trend", "stable");
        sampleData.add(researchCommittee);

        return sampleData;
    }

    public Map<String, Object> getMemberDashboardStats(Long userId, Long subcommitteeId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("assignedTasks", 0);
            stats.put("completedTasks", 0);
            stats.put("upcomingMeetings", 0);
            stats.put("unreadNotifications", 0);
            stats.put("subcommitteePerformance", 0);
            stats.put("recentTasks", new ArrayList<>());

            if (subcommitteeId == null) {
                Optional<com.earacg.earaconnect.model.User> userOpt = userRepo.findById(userId);
                if (userOpt.isPresent() && userOpt.get().getSubcommittee() != null) {
                    subcommitteeId = userOpt.get().getSubcommittee().getId();
                }
            }

            if (subcommitteeId == null) {
                return stats;
            }

            // Assigned Tasks (Resolutions for the subcommittee + Sub-tasks for the user)
            long subcommitteeResolutions = resolutionAssignmentRepo.countBySubcommitteeId(subcommitteeId);
            Long memberId = resolveMemberIdForUser(userId);
            long userSubTasks = memberId != null ? subTaskRepository.countByAssignedToId(memberId) : 0;

            // Completed Tasks
            long completedSubTasks = memberId != null
                    ? subTaskRepository.countByAssignedToIdAndStatus(memberId, com.earacg.earaconnect.model.SubTask.TaskStatus.DONE)
                    : 0;

            // Upcoming Meetings — only count invitations for meetings whose date is in the future
            List<com.earacg.earaconnect.model.MeetingInvitation> userInvitations = meetingInvitationRepo.findByUserId(userId);
            LocalDateTime now = LocalDateTime.now();
            long upcomingMeetings = userInvitations.stream()
                    .filter(inv -> inv.getStatus() == com.earacg.earaconnect.model.MeetingInvitation.InvitationStatus.PENDING
                            || inv.getStatus() == com.earacg.earaconnect.model.MeetingInvitation.InvitationStatus.ACCEPTED)
                    .filter(inv -> inv.getMeeting() != null
                            && inv.getMeeting().getMeetingDate() != null
                            && inv.getMeeting().getMeetingDate().isAfter(now))
                    .count();

            // Unread Notifications
            long unreadNotifications = notificationRepo.countByUserIdAndIsRead(userId, false);

                // Subcommittee Performance
                // Use the same effective-performance resolver used by other dashboards so
                // chair-missed numeric values can still fall back to HOD ranking or task completion.
                List<Report> subReports = reportRepo.findBySubcommitteeId(subcommitteeId);
                List<Report> consideredReports = subReports.stream()
                    .filter(r -> r.getStatus() != Report.ReportStatus.DRAFT)
                    .collect(Collectors.toList());

                double avgPerformance = consideredReports.stream()
                    .mapToDouble(this::resolveMemberReportPerformance)
                    .average()
                    .orElse(0.0);

                if (avgPerformance <= 0.0) {
                avgPerformance = calculateSubcommitteeTaskActivityScore(subcommitteeId);
                }

            stats.put("assignedTasks", subcommitteeResolutions + userSubTasks);
            stats.put("completedTasks", completedSubTasks);
            stats.put("upcomingMeetings", upcomingMeetings);
            stats.put("unreadNotifications", unreadNotifications);
            stats.put("subcommitteePerformance", (int) Math.round(avgPerformance));

            // Recent Tasks (Combining Resolutions and Sub-tasks)
            List<Map<String, Object>> recentTasks = new ArrayList<>();
            // Get last 5 sub-tasks
            if (memberId != null) {
                subTaskRepository.findTop5ByAssignedToIdOrderByCreatedAtDesc(memberId).forEach(t -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("id", t.getId());
                    taskMap.put("title", t.getTitle());
                    taskMap.put("status", t.getStatus());
                    taskMap.put("createdAt", t.getCreatedAt());
                    taskMap.put("taskType", "SUBTASK");
                    recentTasks.add(taskMap);
                });
            }

            stats.put("recentTasks", recentTasks);

        } catch (Exception e) {
            System.err.println("Error calculating member stats: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    private double resolveMemberReportPerformance(Report report) {
        if (report == null) {
            return 0.0;
        }

        if (report.getPerformancePercentage() != null && report.getPerformancePercentage() > 0) {
            return Math.max(0.0, Math.min(100.0, report.getPerformancePercentage()));
        }

        if (report.getHodRanking() != null && report.getHodRanking() > 0) {
            return Math.max(0.0, Math.min(100.0, report.getHodRanking() * 20.0));
        }

        if (report.getStatus() == Report.ReportStatus.APPROVED_BY_HOD
                || report.getStatus() == Report.ReportStatus.APPROVED_BY_COMMISSIONER) {
            return 60.0;
        }

        return 0.0;
    }

    private double calculateSubcommitteeTaskActivityScore(Long subcommitteeId) {
        List<com.earacg.earaconnect.model.SubTask> tasks = subTaskRepository.findBySubcommitteeId(subcommitteeId);
        if (tasks == null || tasks.isEmpty()) {
            return 0.0;
        }

        double totalScore = tasks.stream().mapToDouble(task -> {
            if (task == null || task.getStatus() == null) {
                return 0.0;
            }
            return switch (task.getStatus()) {
                case DONE -> 100.0;
                case IN_PROGRESS -> 60.0;
                case TODO -> 10.0;
            };
        }).sum();

        return Math.round((totalScore / tasks.size()) * 100.0) / 100.0;
    }

    /**
     * Map a User (auth account) to a CSubCommitteeMembers record (task assignment).
     * This is needed because SubTask.assignedTo references CSubCommitteeMembers, not User.
     */
    private Long resolveMemberIdForUser(Long userId) {
        if (userId == null) return null;
        var userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getEmail() == null) return null;
        var user = userOpt.get();

        List<com.earacg.earaconnect.model.CSubCommitteeMembers> matches =
                cSubCommitteeMembersRepo.findByEmailNormalized(user.getEmail());
        if (matches == null || matches.isEmpty()) {
            matches = cSubCommitteeMembersRepo.findByEmailIgnoreCase(user.getEmail());
        }
        if (matches == null || matches.isEmpty()) return null;

        if (user.getSubcommittee() != null && user.getSubcommittee().getId() != null) {
            Long subId = user.getSubcommittee().getId();
            for (var m : matches) {
                if (m != null && m.getSubCommittee() != null && subId.equals(m.getSubCommittee().getId())) {
                    return m.getId();
                }
            }
        }
        return matches.get(0) != null ? matches.get(0).getId() : null;
    }

    private Map<String, Object> getSampleMonthlyOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("approvalRate", 85.5);
        overview.put("totalReviews", 15);
        overview.put("totalReports", 105);
        overview.put("totalResolutions", 91);
        return overview;
    }

    public Map<String, Object> getAdminStats(Integer year, String status) {
        Map<String, Object> stats = new HashMap<>();

        try {
            LocalDateTime start = LocalDateTime.MIN;
            LocalDateTime end = LocalDateTime.MAX;

            if (year != null) {
                start = LocalDateTime.of(year, 1, 1, 0, 0);
                end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
            }

            long totalMeetings;
            long totalResolutions;
            long pendingApprovals;

            if (status != null && !status.equalsIgnoreCase("all")) {
                // Filter by status implementation
                // Mapping generic "status" to specific Enum types might be complex
                // For now, if status is provided, we might interpret it as "ACTIVE"/"APPROVED"
                // vs "ALL"
                // But the requirement suggests Year filter is primary.

                // If specific status filtering is needed, we'd need to parse the status string
                // defaulting to year filter only for now as per plan
                totalMeetings = meetingRepo.countByMeetingDateBetween(start, end);
                totalResolutions = resolutionRepo.countByCreatedAtBetween(start, end);
            } else {
                totalMeetings = meetingRepo.countByMeetingDateBetween(start, end);
                totalResolutions = resolutionRepo.countByCreatedAtBetween(start, end);
            }

            // Pending approvals usually don't depend on year (it's current state), but
            // maybe created date?
            // Let's keep pending approvals as global for now, or filter if requested.
            pendingApprovals = reportRepo.countByStatus(Report.ReportStatus.SUBMITTED);

            stats.put("totalMeetings", totalMeetings);
            stats.put("totalResolutions", totalResolutions);
            stats.put("pendingApprovals", pendingApprovals);

        } catch (Exception e) {
            System.err.println("Error getting admin stats: " + e.getMessage());
            stats.put("totalMeetings", 0);
            stats.put("totalResolutions", 0);
            stats.put("pendingApprovals", 0);
        }

        return stats;
    }

    /**
     * Get members who have been rated highly (chairRanking >= 4) for 3 consecutive tasks.
     * Returns a list of maps with member info and a recognition message.
     */
    public List<Map<String, Object>> getHighPerformingMembers() {
        List<Map<String, Object>> highPerformers = new ArrayList<>();
        try {
            // Get all sub-committee members
            List<com.earacg.earaconnect.model.CSubCommitteeMembers> allMembers = cSubCommitteeMembersRepo.findAll();

            for (com.earacg.earaconnect.model.CSubCommitteeMembers member : allMembers) {
                List<com.earacg.earaconnect.model.SubTask> tasks = subTaskRepository
                        .findByAssignedToIdOrderByCreatedAtDesc(member.getId());

                // Filter tasks that have a chair ranking
                List<com.earacg.earaconnect.model.SubTask> ratedTasks = tasks.stream()
                        .filter(t -> t.getChairRanking() != null)
                        .collect(Collectors.toList());

                if (ratedTasks.size() < 3)
                    continue;

                // Check if the 3 most recent rated tasks all have chairRanking >= 4
                boolean threeConsecutiveHigh = ratedTasks.get(0).getChairRanking() >= 4
                        && ratedTasks.get(1).getChairRanking() >= 4
                        && ratedTasks.get(2).getChairRanking() >= 4;

                if (threeConsecutiveHigh) {
                    double avgRating = ratedTasks.stream()
                            .limit(3)
                            .mapToInt(com.earacg.earaconnect.model.SubTask::getChairRanking)
                            .average().orElse(0.0);

                    String subcommitteeName = member.getSubCommittee() != null
                            ? member.getSubCommittee().getName()
                            : "Unknown";

                    Map<String, Object> entry = new HashMap<>();
                    entry.put("memberId", member.getId());
                    entry.put("memberName", member.getName());
                    entry.put("email", member.getEmail());
                    entry.put("subcommittee", subcommitteeName);
                    entry.put("consecutiveHighRatings", 3);
                    entry.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
                    entry.put("recognitionMessage",
                            "Thank you, " + member.getName() + ", for your outstanding contributions on behalf of the "
                                    + subcommitteeName
                                    + " subcommittee. Your consistent high performance is greatly valued!");

                    // Count total consecutive high ratings (could be more than 3)
                    int consecutiveCount = 0;
                    for (com.earacg.earaconnect.model.SubTask t : ratedTasks) {
                        if (t.getChairRanking() >= 4)
                            consecutiveCount++;
                        else
                            break;
                    }
                    entry.put("consecutiveHighRatings", consecutiveCount);

                    highPerformers.add(entry);
                }
            }

            // Sort by average rating descending
            highPerformers.sort((a, b) -> Double.compare(
                    (Double) b.get("averageRating"),
                    (Double) a.get("averageRating")));

        } catch (Exception e) {
            System.err.println("Error getting high performing members: " + e.getMessage());
            e.printStackTrace();
        }
        return highPerformers;
    }
}