package com.earacg.earaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.SubCommittee;
import java.util.Optional;

@Repository
public interface SubCommitteeRepo extends JpaRepository<SubCommittee, Long>{
    
    /**
     * Find subcommittee by name
     */
    Optional<SubCommittee> findByName(String name);
    
}
