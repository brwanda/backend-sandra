package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.service.EmailService;
import com.earacg.earaconnect.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// DISABLED IN PRODUCTION — test endpoints expose unsafe operations
// @RestController
// @RequestMapping("/api/test")
public class TestController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private MeetingService meetingService;

    /**
     * Test the invitation system without authentication
     */
    @PostMapping("/invitation-test")
    public ResponseEntity<?> testInvitationSystem(@RequestBody Map<String, Object> request) {
        try {
            Long meetingId = Long.valueOf(request.get("meetingId").toString());
            List<Long> userIds = ((List<?>) request.get("userIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .toList();

            System.out.println("🧪 Test invitation system:");
            System.out.println("   Meeting ID: " + meetingId);
            System.out.println("   User IDs: " + userIds);

            // Test the invitation system
            meetingService.sendInvitationsToUsers(meetingId, userIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test invitations sent successfully",
                    "meetingId", meetingId,
                    "userCount", userIds.size(),
                    "timestamp", java.time.LocalDateTime.now().toString()));

        } catch (Exception e) {
            System.err.println("❌ Test invitation failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Test invitation failed: " + e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }

    /**
     * Test email sending directly
     */
    @PostMapping("/email-test")
    public ResponseEntity<?> testEmailSending(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String name = request.get("name");
            String meetingTitle = request.get("meetingTitle");
            String meetingDate = request.get("meetingDate");
            String location = request.get("location");

            System.out.println("🧪 Test email sending:");
            System.out.println("   To: " + email);
            System.out.println("   Name: " + name);
            System.out.println("   Meeting: " + meetingTitle);

            emailService.sendMeetingInvitation(email, name, meetingTitle, meetingDate, location);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent successfully",
                    "email", email,
                    "timestamp", java.time.LocalDateTime.now().toString()));

        } catch (Exception e) {
            System.err.println("❌ Test email failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Test email failed: " + e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }

    /**
     * Get available meetings for testing
     */
    @GetMapping("/meetings")
    public ResponseEntity<?> getTestMeetings() {
        try {
            // This would need to be implemented in MeetingService
            // For now, return a simple response
            return ResponseEntity.ok(Map.of(
                    "message", "Test endpoint working",
                    "timestamp", java.time.LocalDateTime.now().toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get test meetings: " + e.getMessage()));
        }
    }
}
