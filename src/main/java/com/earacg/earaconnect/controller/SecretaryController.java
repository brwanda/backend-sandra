package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Meeting;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.Resolution;
import com.earacg.earaconnect.model.MeetingInvitation;
import com.earacg.earaconnect.repository.ResolutionRepo;
import com.earacg.earaconnect.repository.MeetingInvitationRepo;
import com.earacg.earaconnect.service.MeetingService;
import com.earacg.earaconnect.service.ResolutionService;
import com.earacg.earaconnect.service.SecretaryValidationService;
import com.earacg.earaconnect.repository.CommitteeRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import com.earacg.earaconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretary")
@RequiredArgsConstructor
public class SecretaryController {

    private final MeetingService meetingService;
    private final ResolutionService resolutionService;
    private final SecretaryValidationService secretaryValidationService;
    private final UserService userService;
    private final ResolutionRepo resolutionRepo;
    private final MeetingInvitationRepo meetingInvitationRepo;
    private final CommitteeRepo committeeRepo;
    private final SubCommitteeRepo subCommitteeRepo;

    @GetMapping("/meetings/{secretaryId}")
    public ResponseEntity<?> getSecretaryMeetings(@PathVariable Long secretaryId) {
        try {
            List<Meeting> meetings = meetingService.getMeetingsForSecretary(secretaryId);
            return ResponseEntity.ok(meetings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to retrieve meetings"));
        }
    }

    @GetMapping("/resolutions/{secretaryId}")
    public ResponseEntity<?> getSecretaryResolutions(@PathVariable Long secretaryId) {
        try {
            List<Meeting> meetings = meetingService.getMeetingsForSecretary(secretaryId);
            java.util.List<Long> meetingIds = meetings.stream()
                    .map(Meeting::getId)
                    .collect(java.util.stream.Collectors.toList());

            List<Resolution> resolutions = resolutionRepo.findAll().stream()
                    .filter(r -> (r.getCreatedBy() != null && r.getCreatedBy().getId().equals(secretaryId)) ||
                            (r.getMeeting() != null && meetingIds.contains(r.getMeeting().getId())))
                    .sorted((r1, r2) -> {
                        if (r1.getCreatedAt() != null && r2.getCreatedAt() != null) {
                            return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                        }
                        return r2.getId().compareTo(r1.getId());
                    })
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(resolutions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to retrieve resolutions"));
        }
    }

    @GetMapping("/meetings/{secretaryId}/validate/{meetingId}")
    public ResponseEntity<?> validateMeetingAccess(
            @PathVariable Long secretaryId,
            @PathVariable Long meetingId) {
        try {
            boolean hasAccess = meetingService.validateSecretaryMeetingAccess(secretaryId, meetingId);

            if (hasAccess) {
                return ResponseEntity.ok(Map.of(
                        "hasAccess", true,
                        "message", "Secretary has access to this meeting"));
            } else {
                return ResponseEntity.ok(Map.of(
                        "hasAccess", false,
                        "message", "Secretary cannot access this meeting due to location restrictions"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "hasAccess", false,
                    "error", e.getMessage()));
        }
    }

    @PutMapping("/meetings/{meetingId}/minutes")
    public ResponseEntity<?> updateMeetingMinutes(
            @PathVariable Long meetingId,
            @RequestBody Map<String, Object> request) {
        try {
            String minutes = (String) request.get("minutes");
            Long secretaryId = Long.valueOf(request.get("secretaryId").toString());

            Meeting updatedMeeting = meetingService.updateMeetingMinutesWithValidation(
                    meetingId, minutes, secretaryId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting minutes updated successfully",
                    "meeting", updatedMeeting));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to update meeting minutes"));
        }
    }

    @PostMapping("/resolutions/{resolutionId}/assign")
    public ResponseEntity<?> assignResolution(
            @PathVariable Long resolutionId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) request.get("assignments");
            Long secretaryId = Long.valueOf(request.get("secretaryId").toString());

            int totalPercentage = assignments.stream()
                    .mapToInt(assignment -> {
                        Object pct = assignment.get("contributionPercentage");
                        if (pct instanceof Number) return ((Number) pct).intValue();
                        return Integer.parseInt(pct.toString());
                    })
                    .sum();

            if (totalPercentage != 100) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error",
                        "Total contribution percentage must equal 100%. Current total: " + totalPercentage + "%"));
            }

            resolutionService.assignResolutionToSubcommittees(resolutionId, assignments, secretaryId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resolution assigned successfully",
                    "assignmentCount", assignments.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to assign resolution"));
        }
    }

    @GetMapping("/validate/{userId}")
    public ResponseEntity<?> validateSecretary(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "error", "User not found"));
            }

            boolean isSecretary = secretaryValidationService.isSecretary(user);
            boolean hasCountry = user.getCountry() != null;

            return ResponseEntity.ok(Map.of(
                    "valid", isSecretary && hasCountry,
                    "isSecretary", isSecretary,
                    "hasCountry", hasCountry,
                    "country", hasCountry ? user.getCountry().getName() : null,
                    "message", getValidationMessage(isSecretary, hasCountry)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", e.getMessage()));
        }
    }

    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(@RequestBody Meeting meeting) {
        try {
            Meeting createdMeeting = meetingService.createMeeting(meeting, null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting created successfully",
                    "meeting", createdMeeting));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to create meeting"));
        }
    }

    @GetMapping("/dashboard/{secretaryId}")
    public ResponseEntity<?> getDashboardStats(@PathVariable Long secretaryId) {
        try {
            User secretary = userService.getUserById(secretaryId).orElse(null);
            if (secretary == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Secretary not found"));
            }

            if (!secretaryValidationService.isSecretary(secretary)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a secretary"));
            }

            List<Meeting> meetings = meetingService.getMeetingsForSecretary(secretaryId);
            long upcomingMeetings = meetings.stream()
                    .filter(m -> m.getMeetingDate().isAfter(java.time.LocalDateTime.now()))
                    .count();
            long completedMeetings = meetings.stream()
                    .filter(m -> m.getStatus() == Meeting.MeetingStatus.COMPLETED)
                    .count();

            java.util.List<Long> meetingIds = meetings.stream().map(Meeting::getId)
                    .collect(java.util.stream.Collectors.toList());

            long pendingResolutions = resolutionRepo.findAll().stream()
                    .filter(r -> r.getStatus() != Resolution.ResolutionStatus.COMPLETED &&
                            (r.getCreatedBy() != null && r.getCreatedBy().getId().equals(secretaryId) ||
                                    (r.getMeeting() != null && meetingIds.contains(r.getMeeting().getId()))))
                    .count();

            long totalInvitationsSent = meetingInvitationRepo.findAll().stream()
                    .filter(inv -> inv.getMeeting() != null && meetingIds.contains(inv.getMeeting().getId()))
                    .count();

            long activeCommittees = committeeRepo.count();
            long totalSubcommittees = subCommitteeRepo.count();

            long scopedCommitteeCount = meetings.stream()
                    .map(Meeting::getCommittee)
                    .filter(java.util.Objects::nonNull)
                    .map(c -> c.getId())
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();

            long scopedSubcommitteeCount = meetings.stream()
                    .map(Meeting::getSubCommittee)
                    .filter(java.util.Objects::nonNull)
                    .map(sc -> sc.getId())
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();

            activeCommittees = Math.max(activeCommittees, scopedCommitteeCount);
            totalSubcommittees = Math.max(totalSubcommittees, scopedSubcommitteeCount);

            return ResponseEntity.ok(Map.of(
                    "totalMeetings", meetings.size(),
                    "upcomingMeetingsCount", upcomingMeetings,
                    "completedMeetings", completedMeetings,
                    "pendingResolutionsCount", pendingResolutions,
                    "activeCommittees", activeCommittees,
                    "subcommittees", totalSubcommittees,
                    "totalInvitationsSent", totalInvitationsSent,
                    "locationValid", secretary.getCountry() != null,
                    "country", secretary.getCountry() != null ? secretary.getCountry().getName() : null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to load dashboard stats"));
        }
    }

    private String getValidationMessage(boolean isSecretary, boolean hasCountry) {
        if (!isSecretary) {
            return "User is not a secretary";
        }
        if (!hasCountry) {
            return "Secretary must have a country assigned for location-based access";
        }
        return "Secretary validation passed";
    }
}
