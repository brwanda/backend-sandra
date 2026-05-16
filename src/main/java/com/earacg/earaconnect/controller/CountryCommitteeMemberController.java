package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.CountryCommitteeMember;
import com.earacg.earaconnect.service.CSubCommitteeMembersService;
import com.earacg.earaconnect.service.CountryCommitteeMemberService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import lombok.*;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/commissioner-generals")
@RequiredArgsConstructor
@Slf4j
public class CountryCommitteeMemberController {

    @Autowired
    private CountryCommitteeMemberService countryCommitteeMemberService;

    @Autowired
    private CSubCommitteeMembersService cSubCommitteeMembersService;

    @PostMapping("/add")
    public ResponseEntity<?> create(@RequestBody CountryCommitteeMember member) {
        try {
            return ResponseEntity.ok(countryCommitteeMemberService.save(member));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/get-by/{id}")
    public ResponseEntity<CountryCommitteeMember> getById(@PathVariable Long id) {
        Optional<CountryCommitteeMember> member = countryCommitteeMemberService.findById(id);
        return member.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<CountryCommitteeMember>> getAll() {
        return ResponseEntity.ok(countryCommitteeMemberService.findAll());
    }

    @GetMapping("/hod")
    public ResponseEntity<List<CountryCommitteeMember>> getHodMembers() {
        return ResponseEntity.ok(countryCommitteeMemberService.getHodMembers());
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody CountryCommitteeMember member) {
        try {
            return ResponseEntity.ok(countryCommitteeMemberService.update(member));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        countryCommitteeMemberService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

        /**
     * Get all members from a specific committee by committee ID
     * GET /api/commissioner-generals/committee/{committeeId}
     */
    @GetMapping("/committee/{committeeId}")
    public ResponseEntity<List<CountryCommitteeMember>> getMembersByCommittee(
            @PathVariable Long committeeId) {
        try {
            List<CountryCommitteeMember> members = countryCommitteeMemberService.getMembersByCommitteeId(committeeId);
            if (members.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            log.error("Error fetching members for committee {}: {}", committeeId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all chairs from a specific committee
     * GET /api/committee-members/committee/{committeeId}/chairs
     */
    @GetMapping("/committee/{committeeId}/chairs")
    public ResponseEntity<List<CountryCommitteeMember>> getChairsByCommittee(
            @PathVariable Long committeeId) {
        try {
            List<CountryCommitteeMember> chairs = countryCommitteeMemberService.getChairsByCommitteeId(committeeId);
            return ResponseEntity.ok(chairs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all vice chairs from a specific committee
     * GET /api/committee-members/committee/{committeeId}/vice-chairs
     */
    @GetMapping("/committee/{committeeId}/vice-chairs")
    public ResponseEntity<List<CountryCommitteeMember>> getViceChairsByCommittee(
            @PathVariable Long committeeId) {
        try {
            List<CountryCommitteeMember> viceChairs = countryCommitteeMemberService.getViceChairsByCommitteeId(committeeId);
            return ResponseEntity.ok(viceChairs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all secretaries from a specific committee
     * GET /api/committee-members/committee/{committeeId}/secretaries
     */
    @GetMapping("/committee/{committeeId}/secretaries")
    public ResponseEntity<List<CountryCommitteeMember>> getSecretariesByCommittee(
            @PathVariable Long committeeId) {
        try {
            List<CountryCommitteeMember> secretaries = countryCommitteeMemberService.getSecretariesByCommitteeId(committeeId);
            return ResponseEntity.ok(secretaries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all regular members from a specific committee
     * GET /api/committee-members/committee/{committeeId}/members
     */
    @GetMapping("/committee/{committeeId}/members")
    public ResponseEntity<List<CountryCommitteeMember>> getRegularMembersByCommittee(
            @PathVariable Long committeeId) {
        try {
            List<CountryCommitteeMember> members = countryCommitteeMemberService.getRegularMembersByCommitteeId(committeeId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a specific committee member by ID
     * GET /api/committee-members/{memberId}
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<CountryCommitteeMember> getMemberById(@PathVariable Long memberId) {
        try {
            Optional<CountryCommitteeMember> member = countryCommitteeMemberService.getMemberById(memberId);
            return member.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all committee members
     * GET /api/committee-members
     */
    @GetMapping
    public ResponseEntity<List<CountryCommitteeMember>> getAllMembers() {
        try {
            List<CountryCommitteeMember> members = countryCommitteeMemberService.getAllMembers();
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/by-country/{countryId}")
    public ResponseEntity<List<CountryCommitteeMember>> getCountryCommitteeMembers(
            @PathVariable Long countryId) {
        return ResponseEntity.ok(countryCommitteeMemberService.findByCountryId(countryId));
    }

    @GetMapping("/subcommittee/{countryId}")
    public ResponseEntity<List<CSubCommitteeMembers>> getCountrySubCommitteeMembers(
            @PathVariable Long countryId) {
        return ResponseEntity.ok(cSubCommitteeMembersService.findByCountryId(countryId));
    }

    @GetMapping("/subcommittee/secretaries/{countryId}")
    public ResponseEntity<List<CSubCommitteeMembers>> getCountryDelegationSecretaries(
            @PathVariable Long countryId) {
        return ResponseEntity.ok(cSubCommitteeMembersService.findDelegationSecretariesByCountryId(countryId));
    }
    
    /**
     * Manually resend credentials for a Commissioner General
     * POST /api/commissioner-generals/{memberId}/resend-credentials
     */
    @PostMapping("/{memberId}/resend-credentials")
    public ResponseEntity<?> resendCredentials(@PathVariable Long memberId) {
        try {
            boolean success = countryCommitteeMemberService.resendCredentials(memberId);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Credentials sent successfully to Commissioner General"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to send credentials. Please check member details and email configuration."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error resending credentials: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Check password status for a Commissioner General
     * GET /api/commissioner-generals/{memberId}/password-status
     */
    @GetMapping("/{memberId}/password-status")
    public ResponseEntity<?> getPasswordStatus(@PathVariable Long memberId) {
        try {
            Map<String, Object> status = countryCommitteeMemberService.getPasswordStatus(memberId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error checking password status: " + e.getMessage()
            ));
        }
    }
    
} 