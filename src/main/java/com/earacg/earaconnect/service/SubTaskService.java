package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.SubTask;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.SubTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubTaskService {

    private final SubTaskRepository subTaskRepository;

    public List<SubTask> getSubTasksBySubcommittee(Long subcommitteeId) {
        return subTaskRepository.findBySubcommitteeId(subcommitteeId);
    }

    public List<SubTask> getSubTasksByMember(Long memberId) {
        return subTaskRepository.findByAssignedToId(memberId);
    }

    public List<SubTask> getSubTasksForUser(User user) {
        if (user.getRole() == User.UserRole.CHAIR) {
            if (user.getSubcommittee() != null) {
                return subTaskRepository.findBySubcommitteeId(user.getSubcommittee().getId());
            }
        } else if (user.getRole() == User.UserRole.SUBCOMMITTEE_MEMBER) {
            return subTaskRepository.findByAssignedToId(user.getId());
        }
        return List.of();
    }

    public List<SubTask> getSubTasksByResolution(Long resolutionId) {
        return subTaskRepository.findByResolutionId(resolutionId);
    }

    public List<SubTask> getSubTasksByResolutionAndSubcommittee(Long resolutionId, Long subcommitteeId) {
        return subTaskRepository.findByResolutionIdAndSubcommitteeId(resolutionId, subcommitteeId);
    }

    public List<SubTask> getSubTasksByMeeting(Long meetingId) {
        return subTaskRepository.findByMeetingId(meetingId);
    }

    @Transactional
    public SubTask createSubTask(SubTask subTask) {
        subTask.setCreatedAt(LocalDateTime.now());
        if (subTask.getStatus() == null) {
            subTask.setStatus(SubTask.TaskStatus.TODO);
        }
        return subTaskRepository.save(subTask);
    }

    @Transactional
    public SubTask updateSubTask(Long id, SubTask taskDetails) {
        SubTask task = subTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        if (taskDetails.getTitle() != null) {
            task.setTitle(taskDetails.getTitle());
        }
        if (taskDetails.getDescription() != null) {
            task.setDescription(taskDetails.getDescription());
        }
        if (taskDetails.getStatus() != null) {
            task.setStatus(taskDetails.getStatus());
        }
        if (taskDetails.getAssignedTo() != null) {
            task.setAssignedTo(taskDetails.getAssignedTo());
        }
        if (taskDetails.getDeadline() != null) {
            task.setDeadline(taskDetails.getDeadline());
        }

        return subTaskRepository.save(task);
    }

    @Transactional
    public SubTask updateMySubTaskProgressNote(Long taskId, User member, String progressNote) {
        SubTask task = subTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(member.getId())) {
            throw new IllegalArgumentException("You can only update your own assigned tasks");
        }

        task.setProgressNote(progressNote);
        task.setStatus(SubTask.TaskStatus.IN_PROGRESS);

        return subTaskRepository.save(task);
    }

    @Transactional
    public SubTask chairRankSubTask(Long taskId, Long chairId, Integer ranking, String feedback) {
        SubTask task = subTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (ranking != null) {
            if (ranking < 1 || ranking > 5) {
                throw new IllegalArgumentException("Ranking must be between 1 and 5");
            }
            task.setChairRanking(ranking);
        }

        if (feedback != null && !feedback.isBlank()) {
            task.setChairFeedback(feedback);
        }

        if (ranking != null) {
            task.setStatus(SubTask.TaskStatus.DONE);
        }

        return subTaskRepository.save(task);
    }

    @Transactional
    public void deleteSubTask(Long id) {
        if (!subTaskRepository.existsById(id)) {
            throw new IllegalArgumentException("Task not found with ID: " + id);
        }
        subTaskRepository.deleteById(id);
    }
}
