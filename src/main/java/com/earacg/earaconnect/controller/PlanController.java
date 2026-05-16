package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Plan;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.service.PlanService;
import com.earacg.earaconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllPlans() {
        try {
            List<Plan> plans = planService.getAllPlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable Long id) {
        return planService.getPlanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/subtask/{subTaskId}")
    public ResponseEntity<?> getPlansBySubTask(@PathVariable Long subTaskId) {
        try {
            List<Plan> plans = planService.getPlansBySubTask(subTaskId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/creator/{createdById}")
    public ResponseEntity<?> getPlansByCreator(@PathVariable Long createdById) {
        try {
            List<Plan> plans = planService.getPlansByCreator(createdById);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPlansByStatus(@PathVariable String status) {
        try {
            Plan.PlanStatus planStatus = Plan.PlanStatus.valueOf(status.toUpperCase());
            List<Plan> plans = planService.getPlansByStatus(planStatus);
            return ResponseEntity.ok(plans);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/subcommittee/{subcommitteeId}")
    public ResponseEntity<?> getPlansBySubcommittee(@PathVariable Long subcommitteeId) {
        try {
            List<Plan> plans = planService.getPlansBySubcommittee(subcommitteeId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody Plan plan, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User user = userService.getUserByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            Plan createdPlan = planService.createPlan(plan, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create plan: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @RequestBody Plan planDetails) {
        try {
            Plan updatedPlan = planService.updatePlan(id, planDetails);
            return ResponseEntity.ok(updatedPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update plan: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updatePlanProgress(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer progressPercentage = request.get("progressPercentage");
            if (progressPercentage == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Progress percentage is required"));
            }

            Plan updatedPlan = planService.updatePlanProgress(id, progressPercentage);
            return ResponseEntity.ok(updatedPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update plan progress: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        try {
            planService.deletePlan(id);
            return ResponseEntity.ok(Map.of("message", "Plan deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete plan: " + e.getMessage()));
        }
    }
}
