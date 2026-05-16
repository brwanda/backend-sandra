package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.service.UserService;
import com.earacg.earaconnect.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpServletRequest request) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            if (authentication != null && authentication.isAuthenticated()) {

                // Preserve original side effects on successful login
                userService.recordSuccessfulLogin(email);

                // Persist authentication in session for subsequent requests
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                // Always write context to session (create session if absent)
                HttpSession httpSession = request.getSession(true);
                httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

                User user = userService.getUserByEmail(email).orElse(null);
                if (user != null) {
                    // Build a safe DTO map — do NOT serialize the raw JPA entity directly.
                    // Direct entity serialization can trigger LazyInitializationException
                    // (Hibernate session closed) or infinite recursion on bidirectional
                    // relationships, both of which produce a 500 with an empty body.
                    Map<String, Object> userDto = new HashMap<>();
                    userDto.put("id", user.getId());
                    userDto.put("email", user.getEmail());
                    userDto.put("name", user.getName());
                    userDto.put("phone", user.getPhone());
                    userDto.put("role", user.getRole() != null ? user.getRole().name() : null);
                    userDto.put("active", user.isActive());

                    // Include subcommittee info for Chair/Member dashboards
                    if (user.getSubcommittee() != null) {
                        Map<String, Object> subcommitteeDto = new HashMap<>();
                        subcommitteeDto.put("id", user.getSubcommittee().getId());
                        subcommitteeDto.put("name", user.getSubcommittee().getName());
                        userDto.put("subcommittee", subcommitteeDto);
                        userDto.put("subcommitteeId", user.getSubcommittee().getId());
                    }

                    // Include country info for HOD scoping
                    if (user.getCountry() != null) {
                        Map<String, Object> countryDto = new HashMap<>();
                        countryDto.put("id", user.getCountry().getId());
                        countryDto.put("name", user.getCountry().getName());
                        userDto.put("country", countryDto);
                        userDto.put("countryId", user.getCountry().getId());
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("user", userDto);
                    return ResponseEntity.ok(response);
                }
            }
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        } catch (org.springframework.security.authentication.DisabledException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Account is inactive. Please contact administrator."));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Account not found. Please check your email."));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        } catch (Exception ignored) {
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            // Get user email from authentication
            String email = authentication.getName();
            User user = userService.getUserByEmail(email).orElse(null);
            
            if (user == null || !user.isActive()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found or inactive"));
            }

            // Build safe DTO map
            Map<String, Object> userDto = new HashMap<>();
            userDto.put("id", user.getId());
            userDto.put("email", user.getEmail());
            userDto.put("name", user.getName());
            userDto.put("phone", user.getPhone());
            userDto.put("role", user.getRole() != null ? user.getRole().name() : null);
            userDto.put("active", user.isActive());

            // Include subcommittee info
            if (user.getSubcommittee() != null) {
                Map<String, Object> subcommitteeDto = new HashMap<>();
                subcommitteeDto.put("id", user.getSubcommittee().getId());
                subcommitteeDto.put("name", user.getSubcommittee().getName());
                userDto.put("subcommittee", subcommitteeDto);
                userDto.put("subcommitteeId", user.getSubcommittee().getId());
            }

            // Include country info
            if (user.getCountry() != null) {
                Map<String, Object> countryDto = new HashMap<>();
                countryDto.put("id", user.getCountry().getId());
                countryDto.put("name", user.getCountry().getName());
                userDto.put("country", countryDto);
                userDto.put("countryId", user.getCountry().getId());
            }

            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Session invalid"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestParam String email, @RequestBody User userDetails) {
        User existingUser = userService.getUserByEmail(email).orElse(null);
        if (existingUser != null) {
            User updatedUser = userService.updateUser(existingUser.getId(), userDetails);
            if (updatedUser != null) {
                return ResponseEntity.ok(updatedUser);
            }
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Failed to update profile"));
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String name = request.get("name");
            String password = request.get("password");

            if (email == null || name == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email, name, and password are required"));
            }

            emailService.sendCredentials(email, name, password);
            return ResponseEntity.ok(Map.of("message", "Test email sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send test email: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        userService.generatePasswordResetToken(email);
        // Always return success to prevent email enumeration
        return ResponseEntity
                .ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");

        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and password are required"));
        }

        boolean success = userService.resetPassword(token, newPassword);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired token."));
        }
    }
}