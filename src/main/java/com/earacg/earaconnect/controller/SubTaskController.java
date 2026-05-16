package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.SubTask;
import com.earacg.earaconnect.service.SubTaskService;
import com.earacg.earaconnect.service.ChairValidationService;
import com.earacg.earaconnect.service.UserService;
import com.earacg.earaconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sub-tasks")
public class SubTaskController {

  @Autowired
  private SubTaskService subTaskService;

  @Autowired
  private ChairValidationService chairValidationService;

  @Autowired
  private UserService userService;

  @GetMapping("/subcommittee/{subcommitteeId}")
  public ResponseEntity<?> getSubTasksBySubcommittee(@PathVariable Long subcommitteeId) {
    try {
      List<SubTask> tasks = subTaskService.getSubTasksBySubcommittee(subcommitteeId);
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/member/{memberId}")
  public ResponseEntity<?> getSubTasksByMember(@PathVariable Long memberId) {
    try {
      List<SubTask> tasks = subTaskService.getSubTasksByMember(memberId);
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/my")
  public ResponseEntity<?> getMySubTasks(java.security.Principal principal) {
    try {
      if (principal == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
      }
      User user = userService.getUserByEmail(principal.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));
      List<SubTask> tasks = subTaskService.getSubTasksForUser(user);
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/resolution/{resolutionId}")
  public ResponseEntity<?> getSubTasksByResolution(@PathVariable Long resolutionId) {
    try {
      List<SubTask> tasks = subTaskService.getSubTasksByResolution(resolutionId);
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get sub-tasks for a resolution scoped to a specific subcommittee */
  @GetMapping("/resolution/{resolutionId}/subcommittee/{subcommitteeId}")
  public ResponseEntity<?> getSubTasksByResolutionAndSubcommittee(
      @PathVariable Long resolutionId,
      @PathVariable Long subcommitteeId) {
    try {
      List<SubTask> tasks = subTaskService.getSubTasksByResolutionAndSubcommittee(resolutionId, subcommitteeId);
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/meeting/{meetingId}")
  public ResponseEntity<?> getSubTasksByMeeting(@PathVariable Long meetingId) {
    try {
      List<SubTask> tasks = subTaskService.getSubTasksByMeeting(meetingId);
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping
  public ResponseEntity<?> createSubTask(@RequestBody SubTask subTask, java.security.Principal principal) {
    try {
      if (principal == null)
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
      User user = userService.getUserByEmail(principal.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));
      Long userId = user.getId();

      // Allow both Chair and Committee Secretary to create sub-tasks
      boolean isChair = chairValidationService.isChair(userId);
      boolean isCommitteeSecretary = user.getRole() == User.UserRole.COMMITTEE_SECRETARY;

      if (!isChair && !isCommitteeSecretary) {
        return ResponseEntity.badRequest().body(Map.of("error", "Only Chair or Committee Secretary can create sub-tasks"));
      }

      if (isChair) {
        subTask.setAssignedByChairId(userId);
      }
      
      SubTask savedTask = subTaskService.createSubTask(subTask);
      return ResponseEntity.ok(savedTask);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateSubTask(@PathVariable Long id, @RequestBody SubTask taskDetails,
      java.security.Principal principal) {
    try {
      if (principal == null)
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
      User user = userService.getUserByEmail(principal.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));

      if (!chairValidationService.isChair(user.getId())) {
        return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
      }

      // Optional: Check if the chair owns the subcommittee of this task
      SubTask updatedTask = subTaskService.updateSubTask(id, taskDetails);
      return ResponseEntity.ok(updatedTask);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{id}/my-status")
  public ResponseEntity<?> updateMyTaskStatus(
      @PathVariable Long id,
      @RequestBody Map<String, String> request,
      java.security.Principal principal) {
    try {
      if (principal == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
      }
      String progressNote = request.get("progressNote");
      if (progressNote == null || progressNote.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Progress Note to Chair of the Subcommittee is required."));
      }

      User user = userService.getUserByEmail(principal.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));
      SubTask updated = subTaskService.updateMySubTaskProgressNote(id, user, progressNote);
      return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", "Failed to update task status: " + e.getMessage()));
    }
  }

  /** Chair ranks a member's subtask work */
  @PutMapping("/{id}/chair-rank")
  public ResponseEntity<?> chairRankSubTask(
      @PathVariable Long id,
      @RequestBody Map<String, String> request,
      java.security.Principal principal) {
    try {
      if (principal == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
      }
      User user = userService.getUserByEmail(principal.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));
      if (!chairValidationService.isChair(user.getId())) {
        return ResponseEntity.badRequest().body(Map.of("error", "Only a Chair can rank member work."));
      }
      Integer ranking = null;
      if (request.get("ranking") != null && !request.get("ranking").isBlank()) {
        ranking = Integer.parseInt(request.get("ranking"));
      }
      String feedback = request.get("feedback");
      SubTask updated = subTaskService.chairRankSubTask(id, user.getId(), ranking, feedback);
      return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", "Failed to rank task: " + e.getMessage()));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteSubTask(@PathVariable Long id, java.security.Principal principal) {
    try {
      if (principal == null)
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
      User user = userService.getUserByEmail(principal.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));

      if (!chairValidationService.isChair(user.getId())) {
        return ResponseEntity.badRequest().body(Map.of("error", "User is not a Chair"));
      }

      subTaskService.deleteSubTask(id);
      return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
