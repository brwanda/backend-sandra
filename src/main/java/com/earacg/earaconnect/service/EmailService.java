package com.earacg.earaconnect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email:billing@elohim-hub.com}")
    private String fromEmail;

    @Value("${app.frontend.url:https://earaconnect.vercel.app}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private void sendEmail(String to, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", "EaraConnect <" + fromEmail + ">");
            payload.put("to", List.of(to));
            payload.put("subject", subject);
            payload.put("text", body);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.resend.com/emails", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email sent successfully to: {}", to);
            } else {
                log.error("❌ Failed to send email to: {}. Status: {}, Body: {}", to, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Error sending email to: {}. Error: {}", to, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendCredentials(String email, String name, String password) {
        log.info("Attempting to send credentials email to: {}", email);
        sendEmail(email,
            "Your EaraConnect System Credentials",
            "Dear " + name + ",\n\n" +
            "Your account has been created in the EaraConnect System.\n\n" +
            "Your login credentials are:\n" +
            "Email: " + email + "\n" +
            "Password: " + password + "\n\n" +
            "Please change your password after your first login.\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public void sendCommissionerGeneralCredentials(String email, String name, String password) {
        log.info("Attempting to send Commissioner General credentials email to: {}", email);
        sendEmail(email,
            "Welcome to EaraConnect - Your Commissioner General Account Credentials",
            "Dear Commissioner General " + name + ",\n\n" +
            "Welcome to the EaraConnect EARA CONNECT Project!\n\n" +
            "Your account has been successfully created with Commissioner General privileges.\n\n" +
            "Your Login Credentials:\n" +
            "Email: " + email + "\n" +
            "Password: " + password + "\n\n" +
            "Login URL: " + frontendUrl + "/login\n\n" +
            "Security Notice:\n" +
            "Please change your password after your first login.\n" +
            "Keep your credentials secure and confidential.\n\n" +
            "Best regards,\nEaraConnect System Administration Team\nEARA CONNECT Project");
    }

    @Async("taskExecutor")
    public void sendHODCredentials(String email, String name, String password) {
        log.info("Attempting to send HOD credentials email to: {}", email);
        sendEmail(email,
            "Welcome to EaraConnect - Your Head of Delegation Account Credentials",
            "Dear Head of Delegation " + name + ",\n\n" +
            "Welcome to the EaraConnect EARA CONNECT Project!\n\n" +
            "Your account has been successfully created with Head of Delegation privileges.\n\n" +
            "Your Login Credentials:\n" +
            "Email: " + email + "\n" +
            "Password: " + password + "\n\n" +
            "Login URL: " + frontendUrl + "/login\n\n" +
            "Security Notice:\n" +
            "Please change your password after your first login.\n\n" +
            "Best regards,\nEaraConnect System Administration Team\nEARA CONNECT Project");
    }

    @Async("taskExecutor")
    public void sendCommitteeMemberCredentials(String email, String name, String password, String role) {
        log.info("Attempting to send {} credentials email to: {}", role, email);
        String roleDisplay = role.replace("_", " ").toLowerCase();
        roleDisplay = roleDisplay.substring(0, 1).toUpperCase() + roleDisplay.substring(1);
        sendEmail(email,
            "Welcome to EaraConnect - Your " + roleDisplay + " Account Credentials",
            "Dear " + roleDisplay + " " + name + ",\n\n" +
            "Welcome to the EaraConnect EARA CONNECT Project!\n\n" +
            "Your account has been successfully created with " + roleDisplay + " privileges.\n\n" +
            "Your Login Credentials:\n" +
            "Email: " + email + "\n" +
            "Password: " + password + "\n\n" +
            "Login URL: " + frontendUrl + "/login\n\n" +
            "Security Notice:\n" +
            "Please change your password after your first login.\n\n" +
            "Best regards,\nEaraConnect System Administration Team\nEARA CONNECT Project");
    }

    @Async("taskExecutor")
    public void sendMeetingInvitation(String email, String name, String meetingTitle, String meetingDate, String location) {
        log.info("Attempting to send meeting invitation email to: {}", email);
        sendEmail(email,
            "Meeting Invitation: " + meetingTitle,
            "Dear " + name + ",\n\n" +
            "You are invited to attend the following meeting:\n\n" +
            "Title: " + meetingTitle + "\n" +
            "Date: " + meetingDate + "\n" +
            "Location: " + location + "\n\n" +
            "Please confirm your attendance in EaraConnect.\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public void sendReportNotification(String email, String name, String reportTitle, String status) {
        log.info("Attempting to send report notification email to: {}", email);
        sendEmail(email,
            "Report Status Update: " + reportTitle,
            "Dear " + name + ",\n\n" +
            "A report for '" + reportTitle + "' has been " + status + ".\n\n" +
            "Please check the EaraConnect system for more details.\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public void sendReportRejectionNotification(String email, String name, String reportTitle, String comments, String reviewerName) {
        log.info("Attempting to send report rejection email to: {}", email);
        sendEmail(email,
            "Report Rejected: " + reportTitle,
            "Dear " + name + ",\n\n" +
            "Your report for '" + reportTitle + "' has been rejected by " + reviewerName + ".\n\n" +
            "Review Comments:\n" + comments + "\n\n" +
            "Please address the feedback and resubmit your report.\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public void sendReportApprovalNotification(String email, String name, String reportTitle, String comments, String reviewerName) {
        log.info("Attempting to send report approval email to: {}", email);
        sendEmail(email,
            "Report Approved: " + reportTitle,
            "Dear " + name + ",\n\n" +
            "Congratulations! Your report for '" + reportTitle + "' has been approved by " + reviewerName + ".\n\n" +
            (comments != null && !comments.trim().isEmpty() ? "Review Comments:\n" + comments + "\n\n" : "") +
            "Your report has been forwarded to the Commissioner General for final review.\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public void sendGeneralNotification(String email, String name, String title, String message) {
        log.info("Attempting to send general notification email to: {}", email);
        sendEmail(email, title,
            "Dear " + name + ",\n\n" + message + "\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public void sendPasswordResetEmail(String email, String name, String resetToken) {
        log.info("Attempting to send password reset email to: {}", email);
        sendEmail(email,
            "EaraConnect Password Reset Request",
            "Dear " + name + ",\n\n" +
            "We received a request to reset your password for your EaraConnect account.\n\n" +
            "Click the link below to reset your password:\n" +
            frontendUrl + "/reset-password?token=" + resetToken + "\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not request a password reset, please ignore this email.\n\n" +
            "Best regards,\nEaraConnect System Team");
    }

    @Async("taskExecutor")
    public boolean testEmailConfiguration() {
        log.info("Testing email configuration...");
        try {
            sendEmail(fromEmail,
                "EaraConnect Email Configuration Test",
                "This is a test email to verify that your EaraConnect email configuration is working correctly.\n\n" +
                "If you receive this email, your email setup is functioning properly.\n\n" +
                "Timestamp: " + java.time.LocalDateTime.now());
            return true;
        } catch (Exception e) {
            log.error("❌ Email configuration test failed: {}", e.getMessage());
            return false;
        }
    }
}