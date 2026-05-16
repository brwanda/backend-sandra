package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Committee;
import com.earacg.earaconnect.repository.CommitteeRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.earacg.earaconnect.model.SubCommittee;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitteeService {

    private final CommitteeRepo committeeRepository;

    @Autowired
    private EntityHistoryService entityHistoryService;

    @Transactional(readOnly = true)
    public List<Committee> getAllCommittees() {
        return committeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Committee getCommitteeById(Long id) {
        return committeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Committee not found with id: " + id));
    }

    @Transactional
    public Committee createCommittee(Committee committee) {
        // Check if committee name already exists
        if (committeeRepository.existsByName(committee.getName())) {
            throw new IllegalArgumentException("Committee with name " + committee.getName() + " already exists");
        }
        return committeeRepository.save(committee);
    }

    @Transactional
    public Committee updateCommittee(Long id, Committee committeeDetails) {
        Committee committee = getCommitteeById(id);
        
        // Check if new name is unique (if changed)
        if (!committee.getName().equals(committeeDetails.getName()) && 
            committeeRepository.existsByName(committeeDetails.getName())) {
            throw new IllegalArgumentException("Committee with name " + committeeDetails.getName() + " already exists");
        }
        
        committee.setName(committeeDetails.getName());
        return committeeRepository.save(committee);
    }

    @Transactional
    public void deleteCommittee(Long id) {
        log.info("🔍 CommitteeService: Attempting to delete committee with ID: {}", id);
        
        try {
            // First, check if committee exists
            Committee committee = getCommitteeById(id);
            log.info("✅ CommitteeService: Found committee: {}", committee.getName());
            
            // Check if there are any subcommittees that reference this committee
            if (committee.getSubCommittees() != null && !committee.getSubCommittees().isEmpty()) {
                log.info("🔍 CommitteeService: Found {} subcommittees that will be deleted due to cascade", committee.getSubCommittees().size());
                for (SubCommittee subCommittee : committee.getSubCommittees()) {
                    log.info("🔍 CommitteeService: Subcommittee to be deleted: {} (ID: {})", subCommittee.getName(), subCommittee.getId());
                }
            } else {
                log.info("🔍 CommitteeService: No subcommittees found for this committee");
            }
            
            // Record deletion history before deleting
            entityHistoryService.recordDeletion("Committee", id, committee, null, null);

            // Attempt to delete the committee (cascade will handle subcommittees)
            committeeRepository.delete(committee);
            log.info("✅ CommitteeService: Successfully deleted committee with ID: {}", id);
            
        } catch (EntityNotFoundException e) {
            log.error("❌ CommitteeService: Committee not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("❌ CommitteeService: Error deleting committee with ID {}: {}", id, e.getMessage());
            log.error("❌ CommitteeService: Exception type: {}", e.getClass().getSimpleName());
            e.printStackTrace();
            throw e;
        }
    }
}