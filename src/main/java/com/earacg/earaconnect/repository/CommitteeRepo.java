package com.earacg.earaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.earacg.earaconnect.model.Committee;

@Repository
public interface CommitteeRepo extends JpaRepository<Committee,Long> {
    boolean existsByName(String name);
    Optional<Committee> findByNameIgnoreCase(String name);
}
