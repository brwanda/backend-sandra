package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.service.ProfilePictureService;
import com.earacg.earaconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfilePictureService profilePictureService;
    private final UserService userService;

    /**
     * Upload profile picture for a user
     * POST /api/profile/{userId}/picture
     */
    @PostMapping("/{userId}/picture")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable Long userId,
            @RequestParam("profilePicture") MultipartFile file) {
        try {
            String profilePictureUrl = profilePictureService.uploadProfilePicture(userId, file);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Profile picture uploaded successfully",
                    "profilePictureUrl", profilePictureUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading profile picture for user {}: {}", userId, e.getMessage());

            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to upload profile picture: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete profile picture for a user
     * DELETE /api/profile/{userId}/picture
     */
    @DeleteMapping("/{userId}/picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable Long userId) {
        try {
            profilePictureService.deleteProfilePicture(userId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Profile picture deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting profile picture for user {}: {}", userId, e.getMessage());

            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to delete profile picture: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get profile picture URL for a user
     * GET /api/profile/{userId}/picture
     */
    @GetMapping("/{userId}/picture")
    public ResponseEntity<?> getProfilePictureUrl(@PathVariable Long userId) {
        try {
            String profilePictureUrl = profilePictureService.getProfilePictureUrl(userId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "profilePictureUrl", profilePictureUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting profile picture URL for user {}: {}", userId, e.getMessage());

            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to get profile picture URL: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user profile by email
     * GET /api/profile/user?email={email}
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserProfile(@RequestParam String email) {
        try {
            User user = userService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user profile for email {}: {}", email, e.getMessage());

            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to get user profile: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update user profile
     * PUT /api/profile/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(userId, userDetails);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "user", updatedUser);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user profile for user {}: {}", userId, e.getMessage());

            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to update profile: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Change user password
     * PUT /api/profile/{userId}/password
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData) {
        try {
            log.info("Changing password for userId: {}", userId);

            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Current password and new password are required"));
            }

            boolean passwordChanged = userService.changePassword(userId, currentPassword, newPassword);

            if (passwordChanged) {
                Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "Password changed successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Current password is incorrect"));
            }
        } catch (Exception e) {
            log.error("Error changing password for userId: {}", userId, e);
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Failed to change password: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
