package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.ResolutionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResolutionAssignmentRepo extends JpaRepository<ResolutionAssignment, Long> {

    List<ResolutionAssignment> findByResolutionId(Long resolutionId);

    List<ResolutionAssignment> findBySubcommitteeId(Long subcommitteeId);

    List<ResolutionAssignment> findByStatus(ResolutionAssignment.AssignmentStatus status);

    List<ResolutionAssignment> findByResolutionIdAndStatus(Long resolutionId,
            ResolutionAssignment.AssignmentStatus status);

    long countBySubcommitteeId(Long subcommitteeId);

}