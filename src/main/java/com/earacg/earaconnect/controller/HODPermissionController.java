package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.service.HODPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for HOD Permission related endpoints
 */
@RestController
@RequestMapping("/api")
public class HODPermissionController {

    @Autowired
    private HODPermissionService hodPermissionService;

    /**
     * Check if a user has HOD privileges
     */
    @GetMapping("/users/{userId}/hod-privileges")
    public ResponseEntity<Map<String, Object>> checkHODPrivileges(@PathVariable Long userId) {
        try {
            boolean hasPrivileges = hodPermissionService.hasHODPrivileges(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("hasHODPrivileges", hasPrivileges);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("hasHODPrivileges", false);
            errorResponse.put("error", "Failed to check HOD privileges");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get Head Of Delegation subcommittee ID
     */
    @GetMapping("/subcommittees/head-of-delegation/id")
    public ResponseEntity<Map<String, Object>> getHeadOfDelegationSubcommitteeId() {
        try {
            Long subcommitteeId = hodPermissionService.getHeadOfDelegationSubcommitteeId();

            Map<String, Object> response = new HashMap<>();
            response.put("subcommitteeId", subcommitteeId);
            response.put("found", subcommitteeId != null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("subcommitteeId", null);
            errorResponse.put("found", false);
            errorResponse.put("error", "Failed to get Head Of Delegation subcommittee ID");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Check if a subcommittee is the Head Of Delegation subcommittee
     */
    @GetMapping("/subcommittees/{subcommitteeId}/is-head-of-delegation")
    public ResponseEntity<Map<String, Object>> isHeadOfDelegationSubcommittee(@PathVariable Long subcommitteeId) {
        try {
            boolean isHeadOfDelegation = hodPermissionService.isHeadOfDelegationSubcommittee(subcommitteeId);

            Map<String, Object> response = new HashMap<>();
            response.put("isHeadOfDelegation", isHeadOfDelegation);
            response.put("subcommitteeId", subcommitteeId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("isHeadOfDelegation", false);
            errorResponse.put("error", "Failed to check if subcommittee is Head Of Delegation");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
