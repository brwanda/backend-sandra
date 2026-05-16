package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlanRepo extends JpaRepository<Plan, Long> {
    
    List<Plan> findBySubTaskId(Long subTaskId);
    
    List<Plan> findByCreatedById(Long createdById);
    
    List<Plan> findByStatus(Plan.PlanStatus status);
    
    List<Plan> findBySubTaskSubcommitteeId(Long subcommitteeId);
}
