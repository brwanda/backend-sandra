package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.repository.UserRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import com.earacg.earaconnect.service.HODPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Debug controller to help troubleshoot HOD permission issues
 * DISABLED IN PRODUCTION — exposes user details without auth
 */
// @RestController
// @RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private SubCommitteeRepo subCommitteeRepo;

    @Autowired
    private HODPermissionService hodPermissionService;

    /**
     * Debug endpoint to check HOD setup
     */
    @GetMapping("/hod-setup")
    public ResponseEntity<Map<String, Object>> checkHODSetup() {
        Map<String, Object> debug = new HashMap<>();

        try {
            // Check Head of Delegation subcommittee
            Optional<SubCommittee> hodSubcommittee = subCommitteeRepo.findByName("Head Of Delegation");
            debug.put("hodSubcommitteeExists", hodSubcommittee.isPresent());
            if (hodSubcommittee.isPresent()) {
                debug.put("hodSubcommitteeId", hodSubcommittee.get().getId());
                debug.put("hodSubcommitteeName", hodSubcommittee.get().getName());
            }

            // Check all Chair users
            List<User> chairs = userRepo.findByRole(User.UserRole.CHAIR);
            debug.put("totalChairs", chairs.size());

            // Check Chair users with subcommittees
            Map<String, Object> chairsInfo = new HashMap<>();
            for (User chair : chairs) {
                Map<String, Object> chairInfo = new HashMap<>();
                chairInfo.put("id", chair.getId());
                chairInfo.put("name", chair.getName());
                chairInfo.put("email", chair.getEmail());
                chairInfo.put("role", chair.getRole());
                chairInfo.put("hasSubcommittee", chair.getSubcommittee() != null);
                if (chair.getSubcommittee() != null) {
                    chairInfo.put("subcommitteeId", chair.getSubcommittee().getId());
                    chairInfo.put("subcommitteeName", chair.getSubcommittee().getName());
                }
                chairInfo.put("hasHODPrivileges", hodPermissionService.hasHODPrivileges(chair));
                chairsInfo.put("chair_" + chair.getId(), chairInfo);
            }
            debug.put("chairs", chairsInfo);

            // Check Vice Chair users
            List<User> viceChairs = userRepo.findByRole(User.UserRole.VICE_CHAIR);
            debug.put("totalViceChairs", viceChairs.size());

            Map<String, Object> viceChairsInfo = new HashMap<>();
            for (User viceChair : viceChairs) {
                Map<String, Object> viceChairInfo = new HashMap<>();
                viceChairInfo.put("id", viceChair.getId());
                viceChairInfo.put("name", viceChair.getName());
                viceChairInfo.put("email", viceChair.getEmail());
                viceChairInfo.put("role", viceChair.getRole());
                viceChairInfo.put("hasSubcommittee", viceChair.getSubcommittee() != null);
                if (viceChair.getSubcommittee() != null) {
                    viceChairInfo.put("subcommitteeId", viceChair.getSubcommittee().getId());
                    viceChairInfo.put("subcommitteeName", viceChair.getSubcommittee().getName());
                }
                viceChairInfo.put("hasHODPrivileges", hodPermissionService.hasHODPrivileges(viceChair));
                viceChairsInfo.put("viceChair_" + viceChair.getId(), viceChairInfo);
            }
            debug.put("viceChairs", viceChairsInfo);

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(debug);
        }
    }

    /**
     * Debug endpoint to check specific user's HOD privileges
     */
    @GetMapping("/user/{userId}/hod-privileges")
    public ResponseEntity<Map<String, Object>> checkUserHODPrivileges(@PathVariable Long userId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                debug.put("error", "User not found");
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            debug.put("userId", user.getId());
            debug.put("userName", user.getName());
            debug.put("userEmail", user.getEmail());
            debug.put("userRole", user.getRole());
            debug.put("hasSubcommittee", user.getSubcommittee() != null);

            if (user.getSubcommittee() != null) {
                debug.put("subcommitteeId", user.getSubcommittee().getId());
                debug.put("subcommitteeName", user.getSubcommittee().getName());
                debug.put("isHeadOfDelegationSubcommittee",
                        "Head Of Delegation".equals(user.getSubcommittee().getName()));
            }

            debug.put("hasHODPrivileges", hodPermissionService.hasHODPrivileges(user));
            debug.put("roleDisplay", hodPermissionService.getUserRoleDisplay(user));

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(debug);
        }
    }
}
