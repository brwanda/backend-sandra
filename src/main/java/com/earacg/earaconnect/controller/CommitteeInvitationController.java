package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/committee-invitations")
public class CommitteeInvitationController {

        @Autowired
        private InvitationService invitationService;

        /**
         * Send invitations to committee members for a specific meeting
         * This endpoint matches the frontend call:
         * /api/committee-invitations/send/{meetingId}
         */
        @PostMapping("/send/{meetingId}")
        public ResponseEntity<?> sendCommitteeInvitations(
                        @PathVariable Long meetingId,
                        @RequestBody Map<String, Object> request) {
                try {
                        // Validate meetingId
                        if (meetingId == null || meetingId <= 0) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error", "Invalid meeting ID: " + meetingId));
                        }

                        @SuppressWarnings("unchecked")
                        List<Integer> committeeIdsInt = (List<Integer>) request.getOrDefault("committees", List.of());
                        List<Long> committeeIds = committeeIdsInt.stream()
                                        .map(Integer::longValue)
                                        .toList();

                        @SuppressWarnings("unchecked")
                        List<Integer> subcommitteeIdsInt = (List<Integer>) request.getOrDefault("subcommittees",
                                        List.of());
                        List<Long> subcommitteeIds = subcommitteeIdsInt.stream()
                                        .map(Integer::longValue)
                                        .toList();

                        // Validate that at least one committee or subcommittee is selected
                        if (committeeIds.isEmpty() && subcommitteeIds.isEmpty()) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error",
                                                                "Please select at least one committee or subcommittee"));
                        }

                        Boolean sendEmail = (Boolean) request.getOrDefault("sendEmail", true);
                        String message = (String) request.get("message");

                        Map<String, Object> result = invitationService.sendBulkInvitations(
                                        meetingId, committeeIds, subcommitteeIds, message, sendEmail);

                        return ResponseEntity.ok(result);
                } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "Invalid number format in request"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error",
                                                        "Failed to send committee invitations: " + e.getMessage()));
                }
        }

        /**
         * Handle OPTIONS request for CORS preflight
         */
        @RequestMapping(value = "/send/{meetingId}", method = RequestMethod.OPTIONS)
        public ResponseEntity<?> handleOptions(@PathVariable Long meetingId) {
                return ResponseEntity.ok()
                                .header("Access-Control-Allow-Origin", "http://localhost:3000","https://earaconnect.vercel.app")
                                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                                .build();
        }

        /**
         * Get invitation status for a meeting
         */
        @GetMapping("/status/{meetingId}")
        public ResponseEntity<?> getInvitationStatus(@PathVariable Long meetingId) {
                try {
                        List<Map<String, Object>> invitations = invitationService.getMeetingInvitations(meetingId);
                        return ResponseEntity.ok(Map.of(
                                        "meetingId", meetingId,
                                        "totalInvitations", invitations.size(),
                                        "invitations", invitations));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "Failed to get invitation status: " + e.getMessage()));
                }
        }
}