package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Plan;
import com.earacg.earaconnect.model.SubTask;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.PlanRepo;
import com.earacg.earaconnect.repository.SubTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepo planRepo;
    private final SubTaskRepository subTaskRepository;

    public List<Plan> getAllPlans() {
        return planRepo.findAll();
    }

    public Optional<Plan> getPlanById(Long id) {
        return planRepo.findById(id);
    }

    public List<Plan> getPlansBySubTask(Long subTaskId) {
        return planRepo.findBySubTaskId(subTaskId);
    }

    public List<Plan> getPlansByCreator(Long createdById) {
        return planRepo.findByCreatedById(createdById);
    }

    public List<Plan> getPlansByStatus(Plan.PlanStatus status) {
        return planRepo.findByStatus(status);
    }

    public List<Plan> getPlansBySubcommittee(Long subcommitteeId) {
        return planRepo.findBySubTaskSubcommitteeId(subcommitteeId);
    }

    @Transactional
    public Plan createPlan(Plan plan, User creator) {
        // Validate subtask exists
        SubTask subTask = subTaskRepository.findById(plan.getSubTask().getId())
                .orElseThrow(() -> new IllegalArgumentException("SubTask not found with ID: " + plan.getSubTask().getId()));

        plan.setSubTask(subTask);
        plan.setCreatedBy(creator);
        plan.setCreatedAt(LocalDateTime.now());
        
        if (plan.getStatus() == null) {
            plan.setStatus(Plan.PlanStatus.DRAFT);
        }
        
        if (plan.getPriority() == null) {
            plan.setPriority(Plan.PlanPriority.MEDIUM);
        }
        
        if (plan.getProgressPercentage() == null) {
            plan.setProgressPercentage(0);
        }

        return planRepo.save(plan);
    }

    @Transactional
    public Plan updatePlan(Long id, Plan planDetails) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));

        if (planDetails.getTitle() != null) {
            plan.setTitle(planDetails.getTitle());
        }
        if (planDetails.getDescription() != null) {
            plan.setDescription(planDetails.getDescription());
        }
        if (planDetails.getStartDate() != null) {
            plan.setStartDate(planDetails.getStartDate());
        }
        if (planDetails.getEndDate() != null) {
            plan.setEndDate(planDetails.getEndDate());
        }
        if (planDetails.getStatus() != null) {
            plan.setStatus(planDetails.getStatus());
        }
        if (planDetails.getPriority() != null) {
            plan.setPriority(planDetails.getPriority());
        }
        if (planDetails.getProgressPercentage() != null) {
            plan.setProgressPercentage(planDetails.getProgressPercentage());
        }
        if (planDetails.getNotes() != null) {
            plan.setNotes(planDetails.getNotes());
        }

        plan.setUpdatedAt(LocalDateTime.now());
        return planRepo.save(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        if (!planRepo.existsById(id)) {
            throw new IllegalArgumentException("Plan not found with ID: " + id);
        }
        planRepo.deleteById(id);
    }

    @Transactional
    public Plan updatePlanProgress(Long id, Integer progressPercentage) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));

        if (progressPercentage < 0 || progressPercentage > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
        }

        plan.setProgressPercentage(progressPercentage);
        
        // Auto-update status based on progress
        if (progressPercentage == 100 && plan.getStatus() != Plan.PlanStatus.COMPLETED) {
            plan.setStatus(Plan.PlanStatus.COMPLETED);
        } else if (progressPercentage > 0 && plan.getStatus() == Plan.PlanStatus.DRAFT) {
            plan.setStatus(Plan.PlanStatus.ACTIVE);
        }

        plan.setUpdatedAt(LocalDateTime.now());
        return planRepo.save(plan);
    }
}
