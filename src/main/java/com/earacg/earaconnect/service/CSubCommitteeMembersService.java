package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.Document;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.model.Committee;
import com.earacg.earaconnect.model.CountryCommitteeMember;
import com.earacg.earaconnect.repository.CountryCommitteeMemberRepo;
import com.earacg.earaconnect.repository.CommitteeRepo;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CSubCommitteeMembersService {

    private final CSubCommitteeMembersRepo repository;
    private final DocumentService documentService;
    private final UserService userService;
    private final CountryCommitteeMemberRepo countryCommitteeMemberRepo;
    private final CountryCommitteeMemberService countryCommitteeMemberService;
    private final CommitteeRepo committeeRepo;


    /**
     * Create new committee member
     */
    public CSubCommitteeMembers create(CSubCommitteeMembers member, MultipartFile appointmentLetter) {
        log.info("[DESERIALIZED] Received member: Name={}, isDelegationSecretary={}, isChair={}, isViceChair={}, isCommitteeSecretary={}, isCommitteeMember={}, AppointedDate={}",
                member.getName(), member.isDelegationSecretary(), member.isChair(), member.isViceChair(),
                member.isCommitteeSecretary(), member.isCommitteeMember(), member.getAppointedDate());
        log.info("Creating member: Name={}, Chair={}, ViceChair={}, DelegationSecretary={}, CommitteeSecretary={}, CommitteeMember={}, AppointedDate={}",
                member.getName(), member.isChair(), member.isViceChair(), member.isDelegationSecretary(),
                member.isCommitteeSecretary(), member.isCommitteeMember(), member.getAppointedDate());

        // Debug subcommittee assignment
        log.info("🔍 CSubCommitteeMembersService: Member subcommittee assignment - SubCommittee: {}", member.getSubCommittee());
        if (member.getSubCommittee() != null) {
            log.info("🔍 CSubCommitteeMembersService: Subcommittee ID: {}, Name: {}", 
                    member.getSubCommittee().getId(), member.getSubCommittee().getName());
        } else {
            log.warn("⚠️ CSubCommitteeMembersService: Member {} has NULL subcommittee assignment!", member.getName());
        }

        // Validate appointment date - must be in the past or today
        validateAppointmentDate(member.getAppointedDate());
        validateSubCommitteeAssignment(member.getSubCommittee());
        
        // Validate that at least one role is assigned
        validateRoleAssignment(member);
        validateMaxTwoRoles(member);

        if (appointmentLetter != null && !appointmentLetter.isEmpty()) {
            Document document = documentService.storeFile(appointmentLetter);
            member.setAppointedLetterDoc(document);
        }

        // Set the user role before saving
        member.setUserRole(member.determineUserRole());
        
        CSubCommitteeMembers savedMember = repository.save(member);
        log.info("✅ CSubCommitteeMembersService: Saved member {} with subcommittee: {}", 
                savedMember.getName(), savedMember.getSubCommittee() != null ? savedMember.getSubCommittee().getName() : "NULL");

        // Sync to users table and send credentials if needed
        syncMemberToUser(savedMember);

        log.info("Successfully saved member: Name={}, ID={}, Chair={}, ViceChair={}, DelegationSecretary={}, CommitteeSecretary={}, CommitteeMember={}, AppointedDate={}",
                savedMember.getName(), savedMember.getId(), savedMember.isChair(), savedMember.isViceChair(),
                savedMember.isDelegationSecretary(), savedMember.isCommitteeSecretary(),
                savedMember.isCommitteeMember(), savedMember.getAppointedDate());

        return savedMember;
    }

    /**
     * Get all committee members with pagination
     */
    @Transactional(readOnly = true)
    public Page<CSubCommitteeMembers> findAll(Pageable pageable) {
        List<CSubCommitteeMembers> allowedMembers = repository.findAll().stream()
                .filter(member -> !isHeadOfDelegationSubCommittee(member.getSubCommittee()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        if (start >= allowedMembers.size()) {
            return new PageImpl<>(List.of(), pageable, allowedMembers.size());
        }
        int end = Math.min(start + pageable.getPageSize(), allowedMembers.size());
        return new PageImpl<>(allowedMembers.subList(start, end), pageable, allowedMembers.size());
    }

    /**
     * Get all committee members
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> findAll() {
        return repository.findAll().stream()
            .filter(member -> !isHeadOfDelegationSubCommittee(member.getSubCommittee()))
            .collect(Collectors.toList());
    }

    /**
     * Get committee member by ID
     */
    @Transactional(readOnly = true)
    public CSubCommitteeMembers findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Committee member not found with id: " + id));
    }

    /**
     * Update committee member
     */
    public CSubCommitteeMembers update(Long id, CSubCommitteeMembers updatedMember, MultipartFile newAppointmentLetter) {
        log.info("[DESERIALIZED] Received member (update): Name={}, isDelegationSecretary={}, isChair={}, isViceChair={}, isCommitteeSecretary={}, isCommitteeMember={}, AppointedDate={}",
                updatedMember.getName(), updatedMember.isDelegationSecretary(), updatedMember.isChair(), updatedMember.isViceChair(),
                updatedMember.isCommitteeSecretary(), updatedMember.isCommitteeMember(), updatedMember.getAppointedDate());
        log.info("Updating member with ID {}: Name={}, Chair={}, ViceChair={}, DelegationSecretary={}, CommitteeSecretary={}, CommitteeMember={}, AppointedDate={}",
                id, updatedMember.getName(), updatedMember.isChair(), updatedMember.isViceChair(),
                updatedMember.isDelegationSecretary(), updatedMember.isCommitteeSecretary(),
                updatedMember.isCommitteeMember(), updatedMember.getAppointedDate());

        CSubCommitteeMembers existingMember = findById(id);

        // Validate appointment date - must be in the past or today
        validateAppointmentDate(updatedMember.getAppointedDate());
        validateSubCommitteeAssignment(updatedMember.getSubCommittee());
        
        // Validate that at least one role is assigned
        validateRoleAssignment(updatedMember);
        validateMaxTwoRoles(updatedMember);

        // Update basic fields
        existingMember.setName(updatedMember.getName());
        existingMember.setPhone(updatedMember.getPhone());
        existingMember.setEmail(updatedMember.getEmail());
        existingMember.setPositionInYourRA(updatedMember.getPositionInYourRA());
        existingMember.setCountry(updatedMember.getCountry());
        existingMember.setSubCommittee(updatedMember.getSubCommittee());
        existingMember.setAppointedDate(updatedMember.getAppointedDate());
        existingMember.setDelegationSecretary(updatedMember.isDelegationSecretary());
        existingMember.setChair(updatedMember.isChair());
        existingMember.setViceChair(updatedMember.isViceChair());
        existingMember.setCommitteeSecretary(updatedMember.isCommitteeSecretary());
        existingMember.setCommitteeMember(updatedMember.isCommitteeMember());
        
        // Update the user role based on new roles
        existingMember.setUserRole(existingMember.determineUserRole());

        // Handle new appointment letter upload
        if (newAppointmentLetter != null && !newAppointmentLetter.isEmpty()) {
            if (existingMember.getAppointedLetterDoc() != null) {
                documentService.deleteDocument(existingMember.getAppointedLetterDoc().getId());
            }
            Document newDocument = documentService.storeFile(newAppointmentLetter);
            existingMember.setAppointedLetterDoc(newDocument);
        }

        CSubCommitteeMembers savedMember = repository.save(existingMember);

        // Sync to users table and update roles if needed
        syncMemberToUser(savedMember);

        log.info("Successfully updated member: Name={}, ID={}, Chair={}, ViceChair={}, DelegationSecretary={}, CommitteeSecretary={}, CommitteeMember={}, AppointedDate={}",
                savedMember.getName(), savedMember.getId(), savedMember.isChair(), savedMember.isViceChair(),
                savedMember.isDelegationSecretary(), savedMember.isCommitteeSecretary(),
                savedMember.isCommitteeMember(), savedMember.getAppointedDate());

        return savedMember;
    }

    /**
     * Delete committee member
     */
    public void delete(Long id) {
        log.info("Deleting committee member with id: {}", id);

        CSubCommitteeMembers member = findById(id);

        // Delete associated document if exists
        if (member.getAppointedLetterDoc() != null) {
            documentService.deleteDocument(member.getAppointedLetterDoc().getId());
        }

        repository.deleteById(id);
    }

    /**
     * Find members by country
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> findByCountryId(Long countryId) {
        return repository.findByCountryId(countryId).stream()
                .filter(member -> !isHeadOfDelegationSubCommittee(member.getSubCommittee()))
                .collect(Collectors.toList());
    }

    /**
     * Find members by sub-committee
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> findBySubCommitteeId(Long subCommitteeId) {
        List<CSubCommitteeMembers> members = repository.findBySubCommitteeId(subCommitteeId);
        if (members.isEmpty()) {
            return members;
        }
        return members.stream()
                .filter(member -> !isHeadOfDelegationSubCommittee(member.getSubCommittee()))
                .collect(Collectors.toList());
    }

    /**
     * Find chairs
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> findChairs() {
        return repository.findByIsChairTrue();
    }

    /**
     * Find vice chairs
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> findViceChairs() {
        return repository.findByIsViceChairTrue();
    }

    /**
     * Find delegation secretaries
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> findDelegationSecretaries() {
        return repository.findByIsDelegationSecretaryTrue();
    }

    /**
     * Search members by name
     */
    @Transactional(readOnly = true)
    public List<CSubCommitteeMembers> searchByName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Check if member exists by email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    public List<CSubCommitteeMembers> findDelegationSecretariesByCountryId(Long countryId) {
        return repository.findByCountryIdAndIsDelegationSecretaryTrue(countryId);
    }

        /**
         * Role occupancy pre-checks are intentionally disabled for subcommittee members so this
         * flow matches committee/HOD role behavior. Backend still enforces role count rules.
         */
    @Transactional(readOnly = true)
    public Map<String, Boolean> getRoleOccupancy(Long countryId, Long subCommitteeId, Long excludeMemberId) {
        Map<String, Boolean> out = new HashMap<>();
        out.put("chairTaken", false);
        out.put("viceChairTaken", false);
        out.put("committeeSecretaryTaken", false);
        out.put("delegationSecretaryTaken", false);
        return out;
    }

    /**
     * Validate that the appointment date is not in the future
     */
    private void validateAppointmentDate(LocalDate appointedDate) {
        if (appointedDate != null && appointedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date cannot be in the future. Please select a date that is today or in the past.");
        }
    }

    /**
     * HOD belongs to committee members, not subcommittee members.
     */
    private void validateSubCommitteeAssignment(SubCommittee subCommittee) {
        if (subCommittee == null) {
            return;
        }
        if (isHeadOfDelegationSubCommittee(subCommittee)) {
            throw new IllegalArgumentException("Head of Delegation must be managed in Committee Members, not Sub-Committee Members.");
        }
    }

    private boolean isHeadOfDelegationSubCommittee(SubCommittee subCommittee) {
        if (subCommittee == null || subCommittee.getName() == null) {
            return false;
        }
        String normalized = subCommittee.getName().trim().toLowerCase();
        return normalized.contains("head of delegation") || normalized.equals("hod");
    }

    /**
     * Repair misplaced HOD data: move a member from subcommittee members table to
     * country committee members table, then remove the source row.
     */
    public CountryCommitteeMember migrateMisplacedHodMember(Long subCommitteeMemberId) {
        CSubCommitteeMembers source = repository.findById(subCommitteeMemberId)
                .orElseThrow(() -> new RuntimeException("Subcommittee member not found with id: " + subCommitteeMemberId));

        if (!isHeadOfDelegationSubCommittee(source.getSubCommittee())) {
            throw new IllegalArgumentException("Selected member is not in Head of Delegation subcommittee.");
        }

        Committee hodCommittee = resolveHodCommittee(source.getSubCommittee());

        CountryCommitteeMember target = countryCommitteeMemberRepo.findByEmailIgnoreCase(source.getEmail()).stream()
                .filter(member -> member.getCommittee() != null && member.getCommittee().getId() != null
                        && member.getCommittee().getId().equals(hodCommittee.getId()))
                .findFirst()
                .orElseGet(CountryCommitteeMember::new);

        target.setName(source.getName());
        target.setPhone(source.getPhone());
        target.setEmail(source.getEmail());
        target.setCountry(source.getCountry());
        target.setCommittee(hodCommittee);
        target.setChair(source.isChair());
        target.setViceChair(source.isViceChair());
        target.setCommitteeSecretary(source.isCommitteeSecretary());
        target.setDelegationSecretary(source.isDelegationSecretary());
        target.setCommitteeMember(source.isCommitteeMember());

        CountryCommitteeMember saved = (target.getId() == null)
                ? countryCommitteeMemberService.save(target)
                : countryCommitteeMemberService.update(target);

        // Use existing delete flow to keep document cleanup consistent.
        delete(source.getId());

        return saved;
    }

    private Committee resolveHodCommittee(SubCommittee subCommittee) {
        if (subCommittee != null && subCommittee.getParentCommittee() != null
                && subCommittee.getParentCommittee().getName() != null
                && subCommittee.getParentCommittee().getName().toLowerCase().contains("head of delegation")) {
            return subCommittee.getParentCommittee();
        }

        return committeeRepo.findByNameIgnoreCase("Head Of Delegation")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Head Of Delegation committee not found. Please create it before migration."));
    }

    /**
     * Validate that at least one role is assigned to the member
     */
    private void validateRoleAssignment(CSubCommitteeMembers member) {
        log.info("Validating role assignment for member: {}", member.getName());
        log.info("Role values - Chair: {}, ViceChair: {}, DelegationSecretary: {}, CommitteeSecretary: {}, CommitteeMember: {}", 
                member.isChair(), member.isViceChair(), member.isDelegationSecretary(), 
                member.isCommitteeSecretary(), member.isCommitteeMember());
        
        if (!member.isChair() && !member.isViceChair() && !member.isDelegationSecretary()
                && !member.isCommitteeSecretary() && !member.isCommitteeMember()) {
            throw new IllegalArgumentException("At least one role must be assigned to the committee member.");
        }
    }

    /**
     * Validate that no more than two roles are assigned and enforce uniqueness rules
     */
    private void validateMaxTwoRoles(CSubCommitteeMembers member) {
        // Validate role count (1-2 roles allowed)
        int count = 0;
        if (member.isChair()) count++;
        if (member.isViceChair()) count++;
        if (member.isDelegationSecretary()) count++;
        if (member.isCommitteeSecretary()) count++;
        if (member.isCommitteeMember()) count++;
        if (count > 2) {
            throw new IllegalArgumentException("A maximum of two roles can be assigned to the committee member.");
        }

        // Validate Chair uniqueness - one per sub-committee
        if (member.isChair() && member.getSubCommittee() != null && member.getSubCommittee().getId() != null) {
            List<CSubCommitteeMembers> existingChairs = repository.findChairsBySubCommitteeId(member.getSubCommittee().getId());
            boolean anotherChairExists = existingChairs.stream()
                .anyMatch(m -> member.getId() == null || !m.getId().equals(member.getId()));
            if (anotherChairExists) {
                String existingChairName = existingChairs.stream()
                    .filter(m -> member.getId() == null || !m.getId().equals(member.getId()))
                    .findFirst()
                    .map(CSubCommitteeMembers::getName)
                    .orElse("Unknown");
                throw new IllegalArgumentException(
                    String.format("Chair role is already assigned to %s in %s sub-committee.", 
                        existingChairName, member.getSubCommittee().getName())
                );
            }
        }

        // Validate Committee Secretary uniqueness - one per sub-committee
        if (member.isCommitteeSecretary() && member.getSubCommittee() != null && member.getSubCommittee().getId() != null) {
            List<CSubCommitteeMembers> existingSecretaries = repository.findSecretariesBySubCommitteeId(member.getSubCommittee().getId());
            boolean anotherSecretaryExists = existingSecretaries.stream()
                .anyMatch(m -> member.getId() == null || !m.getId().equals(member.getId()));
            if (anotherSecretaryExists) {
                String existingSecretaryName = existingSecretaries.stream()
                    .filter(m -> member.getId() == null || !m.getId().equals(member.getId()))
                    .findFirst()
                    .map(CSubCommitteeMembers::getName)
                    .orElse("Unknown");
                throw new IllegalArgumentException(
                    String.format("Committee Secretary role is already assigned to %s in %s sub-committee.", 
                        existingSecretaryName, member.getSubCommittee().getName())
                );
            }
        }

        // Validate Delegation Secretary uniqueness - one per country across ALL committees and sub-committees
        if (member.isDelegationSecretary() && member.getCountry() != null && member.getCountry().getId() != null) {
            // Check in sub-committee members table
            List<CSubCommitteeMembers> existingInSubCommittees = repository.findByCountryIdAndIsDelegationSecretaryTrue(member.getCountry().getId());
            boolean anotherExistsInSubCommittees = existingInSubCommittees.stream()
                .anyMatch(m -> member.getId() == null || !m.getId().equals(member.getId()));
            
            if (anotherExistsInSubCommittees) {
                String existingName = existingInSubCommittees.stream()
                    .filter(m -> member.getId() == null || !m.getId().equals(member.getId()))
                    .findFirst()
                    .map(CSubCommitteeMembers::getName)
                    .orElse("Unknown");
                throw new IllegalArgumentException(
                    String.format("Delegation Secretary is already assigned to %s for %s.", 
                        existingName, member.getCountry().getName())
                );
            }
            
            // Check in committee members table (cross-table validation)
            List<CountryCommitteeMember> existingInCommittees = countryCommitteeMemberRepo.findDelegationSecretariesByCountryId(member.getCountry().getId());
            if (!existingInCommittees.isEmpty()) {
                String existingName = existingInCommittees.get(0).getName();
                throw new IllegalArgumentException(
                    String.format("Delegation Secretary is already assigned to %s for %s in a committee.", 
                        existingName, member.getCountry().getName())
                );
            }
            
            log.info("✅ Delegation Secretary validation passed for {} in {}", 
                member.getName(), member.getCountry().getName());
        }
    }

    /**
     * Sync committee member to users table and assign roles. Creates user if not exists, updates roles if exists.
     */
    private void syncMemberToUser(CSubCommitteeMembers member) {
        if (member.getEmail() == null || member.getEmail().isEmpty()) {
            log.warn("Committee member {} has no email, cannot sync to user.", member.getName());
            return;
        }
        
        // Debug logging for subcommittee assignment
        log.info("🔍 CSubCommitteeMembersService: Syncing member {} to user", member.getName());
        log.info("🔍 CSubCommitteeMembersService: Member subcommittee: {}", member.getSubCommittee());
        if (member.getSubCommittee() != null) {
            log.info("🔍 CSubCommitteeMembersService: Subcommittee ID: {}, Name: {}", 
                    member.getSubCommittee().getId(), member.getSubCommittee().getName());
        } else {
            log.warn("⚠️ CSubCommitteeMembersService: Member {} has NULL subcommittee!", member.getName());
        }
        
        // Determine the primary role from membership boolean flags (single source of truth)
        User.UserRole primaryRole = member.determineUserRole();
        log.info("🔍 CSubCommitteeMembersService: Determined primary role: {}", primaryRole);
        
        // Check if user exists
        Optional<User> userOpt = userService.getUserByEmail(member.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Preserve user-level roles (HOD, COMMISSIONER_GENERAL, ADMIN, SECRETARY)
            // These are set by the admin and should NOT be overridden by membership booleans.
            User.UserRole currentRole = user.getRole();
            boolean isProtectedRole = (currentRole == User.UserRole.HOD
                    || currentRole == User.UserRole.COMMISSIONER_GENERAL
                    || currentRole == User.UserRole.ADMIN
                    || currentRole == User.UserRole.SECRETARY);
            if (!isProtectedRole) {
                user.setRole(primaryRole);
            }
            
            user.setName(member.getName());
            user.setEmail(member.getEmail());
            user.setSubcommittee(member.getSubCommittee());
            user.setCountry(member.getCountry());
            userService.updateUser(user.getId(), user);
            log.info("✅ Updated existing user {}", user.getEmail());
        } else {
            // Create new user - UserService will handle password generation and email sending
            User newUser = new User();
            newUser.setEmail(member.getEmail());
            newUser.setName(member.getName());
            newUser.setPhone(member.getPhone());
            newUser.setRole(primaryRole);
            newUser.setCountry(member.getCountry());
            newUser.setSubcommittee(member.getSubCommittee());
            newUser.setActive(true);
            userService.createUser(newUser);
            log.info("✅ Created new user {}", newUser.getEmail());
        }
    }
    
}