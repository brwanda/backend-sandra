package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.Document;
import com.earacg.earaconnect.model.Committee;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.model.CountryCommitteeMember;
import com.earacg.earaconnect.repository.CommitteeRepo;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import com.earacg.earaconnect.service.CSubCommitteeMembersService;
import com.earacg.earaconnect.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/country-committee-members")
@RequiredArgsConstructor
@Slf4j
public class CSubCommitteeMembersController {

    private final CSubCommitteeMembersService service;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    private final CommitteeRepo committeeRepo;
    private final SubCommitteeRepo subCommitteeRepo;

    /**
     * Create new committee member
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("member") String memberJson,
            @RequestParam(value = "appointmentLetter", required = false) MultipartFile appointmentLetter) {
        try {
            log.info("Received member JSON: {}", memberJson);
            log.info("Received file: {}", appointmentLetter != null ? appointmentLetter.getOriginalFilename() : "none");

            // Parse JSON string to CSubCommitteeMembers object
            CSubCommitteeMembers member = objectMapper.readValue(memberJson, CSubCommitteeMembers.class);

            // Input validation
            if (member.getName() == null || member.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required."));
            }
            if (member.getEmail() == null || member.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
            }
            if (member.getAppointedDate() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Appointed date is required."));
            }
            // Appointed date validation
            if (member.getAppointedDate().isAfter(java.time.LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Appointed date cannot be in the future."));
            }
            // Role selection validation
            int roleCount = 0;
            if (member.isChair())
                roleCount++;
            if (member.isViceChair())
                roleCount++;
            if (member.isCommitteeSecretary())
                roleCount++;
            if (member.isDelegationSecretary())
                roleCount++;
            if (member.isCommitteeMember())
                roleCount++;
            if (roleCount > 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "A member cannot have more than 2 roles."));
            }

            CSubCommitteeMembers createdMember = service.create(member, appointmentLetter);
            // Ensure role is visible in response
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMember);
        } catch (Exception e) {
            log.error("Error creating committee member", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create committee member: " + e.getMessage()));
        }
    }

    /**
     * Update committee member
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMember(
            @PathVariable Long id,
            @RequestParam("member") String memberJson,
            @RequestParam(value = "appointmentLetter", required = false) MultipartFile appointmentLetter) {

        try {
            log.info("Updating member with ID: {}", id);
            log.info("Received member JSON: {}", memberJson);
            log.info("Received file: {}", appointmentLetter != null ? appointmentLetter.getOriginalFilename() : "none");

            // Parse JSON string to CSubCommitteeMembers object
            log.info("About to parse JSON: {}", memberJson);
            CSubCommitteeMembers member = objectMapper.readValue(memberJson, CSubCommitteeMembers.class);

            log.info(
                    "Parsed member object - Name: {}, Chair: {}, ViceChair: {}, DelegationSecretary: {}, CommitteeSecretary: {}, CommitteeMember: {}",
                    member.getName(), member.isChair(), member.isViceChair(), member.isDelegationSecretary(),
                    member.isCommitteeSecretary(), member.isCommitteeMember());

            CSubCommitteeMembers updatedMember = service.update(id, member, appointmentLetter);
            return ResponseEntity.ok(updatedMember);
        } catch (RuntimeException e) {
            log.error("Error updating committee member", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update committee member: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error parsing member data", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid member data format: " + e.getMessage()));
        }
    }

    // Keep all other existing methods unchanged...
    @GetMapping
    public ResponseEntity<Page<CSubCommitteeMembers>> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<CSubCommitteeMembers> members = service.findAll(pageable);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CSubCommitteeMembers>> getAllMembersWithoutPagination() {
        List<CSubCommitteeMembers> members = service.findAll();
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMemberById(@PathVariable Long id) {
        try {
            CSubCommitteeMembers member = service.findById(id);
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMember(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("message", "Committee member deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting committee member", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to delete committee member: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/appointment-letter")
    public ResponseEntity<Resource> downloadAppointmentLetter(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            CSubCommitteeMembers member = service.findById(id);

            if (member.getAppointedLetterDoc() == null) {
                return ResponseEntity.notFound().build();
            }

            Document document = member.getAppointedLetterDoc();
            Resource resource = documentService.loadFileAsResource(document.getStoredFilename());

            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                log.info("Could not determine file type.");
            }

            if (contentType == null) {
                contentType = document.getContentType();
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading appointment letter", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/appointment-letter/view")
    public ResponseEntity<Resource> viewAppointmentLetter(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            CSubCommitteeMembers member = service.findById(id);

            if (member.getAppointedLetterDoc() == null) {
                return ResponseEntity.notFound().build();
            }

            Document document = member.getAppointedLetterDoc();
            Resource resource = documentService.loadFileAsResource(document.getStoredFilename());

            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                log.info("Could not determine file type.");
            }

            if (contentType == null) {
                contentType = document.getContentType();
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error viewing appointment letter", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CSubCommitteeMembers>> searchMembers(
            @RequestParam String name) {
        List<CSubCommitteeMembers> members = service.searchByName(name);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<List<CSubCommitteeMembers>> getMembersByCountry(
            @PathVariable Long countryId) {
        List<CSubCommitteeMembers> members = service.findByCountryId(countryId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/sub-committee/{subCommitteeId}")
    public ResponseEntity<List<CSubCommitteeMembers>> getMembersBySubCommittee(
            @PathVariable Long subCommitteeId) {
        List<CSubCommitteeMembers> members = service.findBySubCommitteeId(subCommitteeId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/chairs")
    public ResponseEntity<List<CSubCommitteeMembers>> getChairs() {
        List<CSubCommitteeMembers> chairs = service.findChairs();
        return ResponseEntity.ok(chairs);
    }

    @GetMapping("/vice-chairs")
    public ResponseEntity<List<CSubCommitteeMembers>> getViceChairs() {
        List<CSubCommitteeMembers> viceChairs = service.findViceChairs();
        return ResponseEntity.ok(viceChairs);
    }

    @GetMapping("/delegation-secretaries")
    public ResponseEntity<List<CSubCommitteeMembers>> getDelegationSecretaries() {
        List<CSubCommitteeMembers> secretaries = service.findDelegationSecretaries();
        return ResponseEntity.ok(secretaries);
    }

    @GetMapping("/email-exists")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        boolean exists = service.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Get which roles are already taken for the given country (and optionally subcommittee).
     * Used by the member form to disable Chair/Vice Chair/Committee Secretary/Delegation Secretary when already assigned.
     * Query params: countryId (required), subCommitteeId (optional, for subcommittee-scoped roles), excludeMemberId (optional, when editing)
     */
    @GetMapping("/role-occupancy")
    public ResponseEntity<Map<String, Boolean>> getRoleOccupancy(
            @RequestParam Long countryId,
            @RequestParam(required = false) Long subCommitteeId,
            @RequestParam(required = false) Long excludeMemberId) {
        Map<String, Boolean> occupancy = service.getRoleOccupancy(countryId, subCommitteeId, excludeMemberId);
        return ResponseEntity.ok(occupancy);
    }

    /**
     * Get member count for a specific subcommittee
     * GET /api/country-committee-members/sub-committee/{subCommitteeId}/count
     */
    @GetMapping("/sub-committee/{subCommitteeId}/count")
    public ResponseEntity<Map<String, Object>> getSubcommitteeMemberCount(@PathVariable Long subCommitteeId) {
        try {
            List<CSubCommitteeMembers> members = service.findBySubCommitteeId(subCommitteeId);
            Map<String, Object> response = Map.of(
                    "subCommitteeId", subCommitteeId,
                    "memberCount", members.size(),
                    "members", members);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting subcommittee member count for ID: " + subCommitteeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get member count for a specific committee
     * GET /api/country-committee-members/committee/{committeeId}/count
     */
    @GetMapping("/committee/{committeeId}/count")
    public ResponseEntity<Map<String, Object>> getCommitteeMemberCount(@PathVariable Long committeeId) {
        try {
            // For now, return all members since the data model stores all members as
            // CSubCommitteeMembers
            // This is a workaround until the committee-member relationship is properly
            // established
            List<CSubCommitteeMembers> allMembers = service.findAll();
            Map<String, Object> response = Map.of(
                    "committeeId", committeeId,
                    "memberCount", allMembers.size(),
                    "members", allMembers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting committee member count for ID: " + committeeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all committees with their member counts
     * GET /api/country-committee-members/committees/with-counts
     */
    @GetMapping("/committees/with-counts")
    public ResponseEntity<List<Map<String, Object>>> getAllCommitteesWithMemberCounts() {
        try {
            List<Committee> committees = committeeRepo.findAll();
            List<Map<String, Object>> committeesWithCounts = new ArrayList<>();

            for (Committee committee : committees) {
                // Determine member count - For now counting all members as a simple metric
                // or we could filter by committee if the relationship was clearer.
                // Given the current model, let's return real names from DB.
                List<CSubCommitteeMembers> allMembers = service.findAll();

                Map<String, Object> committeeMap = new HashMap<>();
                committeeMap.put("id", committee.getId());
                committeeMap.put("name", committee.getName());
                // Distribution logic remains as a fallback or we use real counts if linked
                committeeMap.put("memberCount", allMembers.size() / committees.size());
                committeesWithCounts.add(committeeMap);
            }

            return ResponseEntity.ok(committeesWithCounts);
        } catch (Exception e) {
            log.error("Error getting committees with member counts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all subcommittees with their member counts
     * GET /api/country-committee-members/subcommittees/with-counts
     */
    @GetMapping("/subcommittees/with-counts")
    public ResponseEntity<List<Map<String, Object>>> getAllSubcommitteesWithMemberCounts() {
        try {
            List<SubCommittee> subcommittees = subCommitteeRepo.findAll();
            List<Map<String, Object>> subcommitteesWithCounts = new ArrayList<>();

            for (SubCommittee sub : subcommittees) {
                List<CSubCommitteeMembers> members = service.findBySubCommitteeId(sub.getId());

                Map<String, Object> subWithCount = new HashMap<>();
                subWithCount.put("id", sub.getId());
                subWithCount.put("name", sub.getName());
                subWithCount.put("memberCount", members.size());
                subcommitteesWithCounts.add(subWithCount);
            }

            return ResponseEntity.ok(subcommitteesWithCounts);
        } catch (Exception e) {
            log.error("Error getting subcommittees with member counts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Repair a misplaced HOD member that was saved in subcommittee members.
     * Moves the record into country committee members, then removes source row.
     */
    @PostMapping("/{id}/repair-hod-placement")
    public ResponseEntity<?> repairHodPlacement(@PathVariable Long id) {
        try {
            CountryCommitteeMember migrated = service.migrateMisplacedHodMember(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "HOD member migrated successfully to country committee members.");
            response.put("subCommitteeMemberId", id);
            response.put("countryCommitteeMemberId", migrated.getId());
            response.put("name", migrated.getName());
            response.put("email", migrated.getEmail());
            response.put("committee", migrated.getCommittee() != null ? migrated.getCommittee().getName() : null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error repairing HOD placement for subcommittee member {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to repair HOD placement: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/repair-hod-placement")
    public ResponseEntity<?> repairHodPlacementGet(@PathVariable Long id) {
        return repairHodPlacement(id);
    }
}