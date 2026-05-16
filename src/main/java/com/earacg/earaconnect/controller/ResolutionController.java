package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Resolution;
import com.earacg.earaconnect.dto.ResolutionDTO;
import com.earacg.earaconnect.service.ResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resolutions")
public class ResolutionController {

    @Autowired
    private ResolutionService resolutionService;

    @GetMapping
    public ResponseEntity<List<ResolutionDTO>> getAllResolutions() {
        try {
            List<Resolution> resolutions = resolutionService.getAllResolutions();
            List<ResolutionDTO> resolutionDTOs = resolutions.stream()
                    .map(ResolutionDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resolutionDTOs);
        } catch (Exception e) {
            System.err.println("Error in getAllResolutions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resolution> getResolutionById(@PathVariable Long id) {
        return resolutionService.getResolutionById(id)
                .map(resolution -> ResponseEntity.ok(resolution))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/meeting/{meetingId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ResolutionDTO>> getResolutionsByMeeting(@PathVariable Long meetingId) {
        try {
            List<Resolution> resolutions = resolutionService.getResolutionsByMeeting(meetingId);
            List<ResolutionDTO> resolutionDTOs = resolutions.stream()
                    .map(ResolutionDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resolutionDTOs);
        } catch (Exception e) {
            System.err.println("Error in getResolutionsByMeeting: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ResolutionDTO>> getResolutionsByStatus(@PathVariable String status) {
        try {
            Resolution.ResolutionStatus resolutionStatus = Resolution.ResolutionStatus.valueOf(status.toUpperCase());
            List<Resolution> resolutions = resolutionService.getResolutionsByStatus(resolutionStatus);
            List<ResolutionDTO> resolutionDTOs = resolutions.stream()
                    .map(ResolutionDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resolutionDTOs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error in getResolutionsByStatus: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Resolution> createResolution(@RequestBody Resolution resolution) {
        Resolution createdResolution = resolutionService.createResolution(resolution);
        if (createdResolution != null) {
            return ResponseEntity.ok(createdResolution);
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Resolution> updateResolution(@PathVariable Long id,
            @RequestBody Resolution resolutionDetails) {
        Resolution updatedResolution = resolutionService.updateResolution(id, resolutionDetails);
        if (updatedResolution != null) {
            return ResponseEntity.ok(updatedResolution);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteResolution(@PathVariable Long id) {
        boolean deleted = resolutionService.deleteResolution(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Resolution deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Assign resolution to subcommittees with contribution percentages
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assignResolutionToSubcommittees(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) request.get("assignments");
            resolutionService.assignResolutionToSubcommittees(id, assignments);
            return ResponseEntity.ok(Map.of("message", "Resolution assigned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to assign resolution: " + e.getMessage()));
        }
    }

    /**
     * Get resolution progress statistics
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<?> getResolutionProgress(@PathVariable Long id) {
        try {
            Map<String, Object> progress = resolutionService.getResolutionProgress(id);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get resolution progress: " + e.getMessage()));
        }
    }

    /**
     * Get resolution assignments
     */
    @GetMapping("/{id}/assignments")
    public ResponseEntity<?> getResolutionAssignments(@PathVariable Long id) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid resolution ID"));
            }

            // Check if resolution exists
            Optional<Resolution> resolution = resolutionService.getResolutionById(id);
            if (resolution.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<Map<String, Object>> assignments = resolutionService.getResolutionAssignments(id);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get resolution assignments: " + e.getMessage()));
        }
    }

    /**
     * Create or update resolution assignments
     */
    @PostMapping("/{id}/assignments")
    public ResponseEntity<?> createResolutionAssignments(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid resolution ID"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) request.get("assignments");

            if (assignments == null || assignments.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No assignments provided"));
            }

            Map<String, Object> result = resolutionService.createResolutionAssignments(id, assignments);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create resolution assignments: " + e.getMessage()));
        }
    }

    /**
     * Update resolution status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateResolutionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }

            Resolution.ResolutionStatus resolutionStatus;
            try {
                resolutionStatus = Resolution.ResolutionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status: " + status));
            }

            Resolution updatedResolution = resolutionService.updateResolutionStatus(id, resolutionStatus);
            if (updatedResolution != null) {
                return ResponseEntity.ok(updatedResolution);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update resolution status: " + e.getMessage()));
        }
    }

    /**
     * Get resolutions for a specific subcommittee
     */
    @GetMapping("/subcommittee/{subcommitteeId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ResolutionDTO>> getResolutionsBySubcommittee(@PathVariable Long subcommitteeId) {
        try {
            List<Resolution> resolutions = resolutionService.getResolutionsBySubcommittee(subcommitteeId);
            List<ResolutionDTO> resolutionDTOs = resolutions.stream()
                    .map(r -> new ResolutionDTO(r, subcommitteeId))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resolutionDTOs);
        } catch (Exception e) {
            System.err.println("Error in getResolutionsBySubcommittee: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}