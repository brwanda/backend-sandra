package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.SubCommittee;
import com.earacg.earaconnect.repository.SubCommitteeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubCommitteeService {

    private final SubCommitteeRepo subCommitteeRepository;

    @Autowired
    private EntityHistoryService entityHistoryService;

    public SubCommitteeService(SubCommitteeRepo subCommitteeRepository) {
        this.subCommitteeRepository = subCommitteeRepository;
    }

    // Create
    public SubCommittee createSubCommittee(SubCommittee subCommittee) {
        return subCommitteeRepository.save(subCommittee);
    }

    // Read (all)
    public List<SubCommittee> getAllSubCommittees() {
        return subCommitteeRepository.findAll();
    }

    // Read (by id)
    public Optional<SubCommittee> getSubCommitteeById(Long id) {
        return subCommitteeRepository.findById(id);
    }

    // Update
    public SubCommittee updateSubCommittee(Long id, SubCommittee subCommitteeDetails) {
        SubCommittee subCommittee = subCommitteeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubCommittee not found with id: " + id));

        subCommittee.setName(subCommitteeDetails.getName());
        
        return subCommitteeRepository.save(subCommittee);
    }

    // Delete
    public void deleteSubCommittee(Long id) {
        SubCommittee subCommittee = subCommitteeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubCommittee not found with id: " + id));
        
        entityHistoryService.recordDeletion("SubCommittee", id, subCommittee, null, null);
        subCommitteeRepository.delete(subCommittee);
    }
}