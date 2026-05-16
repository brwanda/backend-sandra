package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.service.DashboardService;
import com.earacg.earaconnect.service.EARAPerformanceDashboardService;
import com.earacg.earaconnect.repository.CountryRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import com.earacg.earaconnect.model.Country;
import com.earacg.earaconnect.model.SubCommittee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private EARAPerformanceDashboardService earaService;

    @Autowired
    private CountryRepo countryRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    @GetMapping("/countries")
    public ResponseEntity<List<String>> getAvailableCountries() {
        List<String> names = countryRepo.findAll().stream()
                .map(Country::getName)
                .sorted()
                .toList();
        return ResponseEntity.ok(names);
    }

    @GetMapping("/committees")
    public ResponseEntity<List<String>> getAvailableCommittees() {
        List<String> names = subCommitteeRepo.findAll().stream()
                .map(SubCommittee::getName)
                .sorted()
                .toList();
        return ResponseEntity.ok(names);
    }

    // NEW ENDPOINTS FOR SIMPLE PERFORMANCE DASHBOARD
    @GetMapping("/available-years")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        try {
            List<Integer> years = dashboardService.getAvailableYears();
            return ResponseEntity.ok(years);
        } catch (Exception e) {
            e.printStackTrace();
            // Return default years if error occurs
            return ResponseEntity.ok(List.of(2024, 2023, 2022));
        }
    }

    @GetMapping("/performance/simple")
    public ResponseEntity<Map<String, Object>> getSimplePerformanceData(@RequestParam Integer year) {
        try {
            Map<String, Object> dashboardData = dashboardService.getSimplePerformanceData(year);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    // EXISTING ENDPOINTS
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceDashboard(
            @RequestParam(required = false, defaultValue = "3months") String timeFilter,
            @RequestParam(required = false, defaultValue = "all") String subcommittee,
            @RequestParam(required = false) Long userId) {

        try {
            Map<String, Object> dashboardData = dashboardService.getPerformanceDashboardData(timeFilter, subcommittee,
                    userId);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/performance/stats")
    public ResponseEntity<Map<String, Object>> getPerformanceStats(
            @RequestParam(required = false) Long hodId,
            @RequestParam(required = false) Long commissionerId) {

        try {
            Map<String, Object> stats = dashboardService.getPerformanceStats(hodId, commissionerId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/subcommittee-performance")
    public ResponseEntity<Map<String, Object>> getSubcommitteePerformance(
            @RequestParam(required = false, defaultValue = "3months") String timeFilter) {

        try {
            Map<String, Object> performance = dashboardService.getSubcommitteePerformanceData(timeFilter);
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/resolution-progress")
    public ResponseEntity<Map<String, Object>> getResolutionProgress() {
        try {
            Map<String, Object> progress = dashboardService.getResolutionProgressData();
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends(
            @RequestParam(required = false, defaultValue = "6") Integer months) {

        try {
            Map<String, Object> trends = dashboardService.getMonthlyTrendsData(months);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status) {
        try {
            Map<String, Object> stats = dashboardService.getAdminStats(year, status);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/comprehensive")
    public ResponseEntity<Map<String, Object>> getComprehensiveDashboard(
            @RequestParam Long userId,
            @RequestParam String userRole) {
        try {
            Map<String, Object> stats = dashboardService.getComprehensiveDashboardData(userId, userRole);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/member/stats")
    public ResponseEntity<Map<String, Object>> getMemberDashboardStats(
            @RequestParam Long userId,
            @RequestParam Long subcommitteeId) {
        try {
            Map<String, Object> stats = dashboardService.getMemberDashboardStats(userId, subcommitteeId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Get members who have been rated highly 3 consecutive times (reward/recognition system) */
    @GetMapping("/high-performers")
    public ResponseEntity<List<Map<String, Object>>> getHighPerformers() {
        try {
            List<Map<String, Object>> highPerformers = dashboardService.getHighPerformingMembers();
            return ResponseEntity.ok(highPerformers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    // ── EARA Performance Dashboard endpoints ──────────────────────────────

    @GetMapping("/eara/performance-metrics")
    public ResponseEntity<Map<String, Object>> getEaraPerformanceMetrics(
            @RequestParam(defaultValue = "all") String country,
            @RequestParam(defaultValue = "all") String committee,
            @RequestParam(defaultValue = "month") String time) {
        try {
            return ResponseEntity.ok(earaService.getPerformanceMetrics(country, committee, time));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eara/country-performance")
    public ResponseEntity<List<Map<String, Object>>> getEaraCountryPerformance(
            @RequestParam(defaultValue = "all") String country,
            @RequestParam(defaultValue = "all") String committee,
            @RequestParam(defaultValue = "month") String time) {
        try {
            return ResponseEntity.ok(earaService.getCountryPerformance(country, committee, time));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eara/resolution-status")
    public ResponseEntity<List<Map<String, Object>>> getEaraResolutionStatus(
            @RequestParam(defaultValue = "all") String country,
            @RequestParam(defaultValue = "all") String committee,
            @RequestParam(defaultValue = "month") String time) {
        try {
            return ResponseEntity.ok(earaService.getResolutionStatus(country, committee, time));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eara/monthly-trends")
    public ResponseEntity<List<Map<String, Object>>> getEaraMonthlyTrends(
            @RequestParam(defaultValue = "all") String country,
            @RequestParam(defaultValue = "all") String committee,
            @RequestParam(defaultValue = "month") String time) {
        try {
            return ResponseEntity.ok(earaService.getMonthlyTrends(country, committee, time));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eara/task-assignments")
    public ResponseEntity<List<Map<String, Object>>> getEaraTaskAssignments(
            @RequestParam(defaultValue = "all") String country,
            @RequestParam(defaultValue = "all") String committee,
            @RequestParam(defaultValue = "month") String time) {
        try {
            return ResponseEntity.ok(earaService.getTaskAssignments(country, committee, time));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eara/gantt-data")
    public ResponseEntity<List<Map<String, Object>>> getEaraGanttData(
            @RequestParam(defaultValue = "all") String country,
            @RequestParam(defaultValue = "all") String committee,
            @RequestParam(defaultValue = "month") String time) {
        try {
            return ResponseEntity.ok(earaService.getGanttData(country, committee, time));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}