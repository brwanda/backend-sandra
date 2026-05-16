package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePictureService {

    private final UserRepo userRepo;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.upload-dir:uploads}/profile-pictures")
    private String profilePictureDir;

    /**
     * Upload profile picture for a user
     */
    public String uploadProfilePicture(Long userId, MultipartFile file) throws IOException {
        // Validate file
        validateProfilePictureFile(file);

        // Get user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Create profile picture directory if it doesn't exist
        Path profilePicturePath = Paths.get(profilePictureDir).toAbsolutePath().normalize();
        Files.createDirectories(profilePicturePath);

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = "profile_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;

        // Store file
        Path targetLocation = profilePicturePath.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Generate URL for the uploaded file
        String profilePictureUrl = "/files/profile-pictures/" + storedFilename;

        // Update user's profile picture URL
        user.setProfilePicture(profilePictureUrl);
        userRepo.save(user);

        log.info("Profile picture uploaded successfully for user {}: {}", userId, profilePictureUrl);
        return profilePictureUrl;
    }

    /**
     * Delete profile picture for a user
     */
    public void deleteProfilePicture(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            try {
                // Extract filename from URL
                String filename = user.getProfilePicture().substring(user.getProfilePicture().lastIndexOf("/") + 1);
                Path filePath = Paths.get(profilePictureDir).resolve(filename);
                
                // Delete file if it exists
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Profile picture file deleted: {}", filePath);
                }

                // Clear profile picture URL from user
                user.setProfilePicture(null);
                userRepo.save(user);
                
                log.info("Profile picture deleted successfully for user: {}", userId);
            } catch (IOException e) {
                log.error("Error deleting profile picture file for user {}: {}", userId, e.getMessage());
                throw new RuntimeException("Failed to delete profile picture file", e);
            }
        }
    }

    /**
     * Get profile picture URL for a user
     */
    public String getProfilePictureUrl(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        return user.getProfilePicture();
    }

    /**
     * Validate profile picture file
     */
    private void validateProfilePictureFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Profile picture file cannot be empty");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed for profile pictures");
        }

        // Check file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("Profile picture file size must be less than 5MB");
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                throw new RuntimeException("Only JPG, JPEG, PNG, GIF, and WebP files are allowed");
            }
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
