// 3. Add this test controller to debug email issues

package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// DISABLED IN PRODUCTION — email test endpoints expose open relay
// @RestController
// @RequestMapping("/api/email-test")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    /**
     * Test email configuration
     */
    @GetMapping("/config")
    public ResponseEntity<?> testEmailConfiguration() {
        try {
            boolean isWorking = emailService.testEmailConfiguration();
            return ResponseEntity.ok(Map.of(
                    "success", isWorking,
                    "message", isWorking ? "Email configuration is working" : "Email configuration failed",
                    "timestamp", java.time.LocalDateTime.now().toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Email configuration test failed: " + e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }

    /**
     * Send a test email
     */
    @PostMapping("/send-test")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String toEmail = request.get("email");
            if (toEmail == null || toEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email address is required"));
            }

            emailService.sendGeneralNotification(
                    toEmail,
                    "Test User",
                    "EaraConnect Email Test",
                    "This is a test email to verify that the email system is working correctly.\n\n" +
                            "If you receive this email, the email configuration is functioning properly.\n\n" +
                            "Timestamp: " + java.time.LocalDateTime.now());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent successfully to " + toEmail,
                    "timestamp", java.time.LocalDateTime.now().toString()));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to send test email: " + e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }

    /**
     * Test meeting invitation email specifically
     */
    @PostMapping("/test-invitation")
    public ResponseEntity<?> testMeetingInvitation(@RequestBody Map<String, String> request) {
        try {
            String toEmail = request.get("email");
            if (toEmail == null || toEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email address is required"));
            }

            emailService.sendMeetingInvitation(
                    toEmail,
                    "Test User",
                    "Test Meeting Invitation",
                    "2024-01-15 10:00:00",
                    "Conference Room A");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting invitation test email sent successfully to " + toEmail,
                    "timestamp", java.time.LocalDateTime.now().toString()));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to send meeting invitation test email: " + e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }
}