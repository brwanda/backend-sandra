package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.service.SubCommitteeService;
import com.earacg.earaconnect.service.CSubCommitteeMembersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sub-committees")
public class SubCommitteeController {

    private final SubCommitteeService subCommitteeService;
    private final CSubCommitteeMembersService cSubCommitteeMembersService;

    public SubCommitteeController(SubCommitteeService subCommitteeService,
            CSubCommitteeMembersService cSubCommitteeMembersService) {
        this.subCommitteeService = subCommitteeService;
        this.cSubCommitteeMembersService = cSubCommitteeMembersService;
    }

    // Create
    @PostMapping
    public SubCommittee createSubCommittee(@RequestBody SubCommittee subCommittee) {
        return subCommitteeService.createSubCommittee(subCommittee);
    }

    // Read (all)
    @GetMapping
    public List<SubCommittee> getAllSubCommittees() {
        return subCommitteeService.getAllSubCommittees();
    }

    // Read (by id)
    @GetMapping("/{id}")
    public ResponseEntity<SubCommittee> getSubCommitteeById(@PathVariable Long id) {
        Optional<SubCommittee> subCommittee = subCommitteeService.getSubCommitteeById(id);
        return subCommittee.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<SubCommittee> updateSubCommittee(@PathVariable Long id,
            @RequestBody SubCommittee subCommitteeDetails) {
        try {
            SubCommittee updatedSubCommittee = subCommitteeService.updateSubCommittee(id, subCommitteeDetails);
            return ResponseEntity.ok(updatedSubCommittee);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubCommittee(@PathVariable Long id) {
        try {
            subCommitteeService.deleteSubCommittee(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/sub-committees/{id}/members
     * Fetches members for a specific subcommittee.
     * This endpoint is called by the EnhancedMemberDashboard in the frontend.
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getSubCommitteeMembers(@PathVariable Long id) {
        try {
            // Use the CSubCommitteeMembersService to find members by subcommittee ID
            List<com.earacg.earaconnect.model.CSubCommitteeMembers> members = cSubCommitteeMembersService
                    .findBySubCommitteeId(id);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to fetch subcommittee members",
                    "details", e.getMessage()));
        }
    }
}