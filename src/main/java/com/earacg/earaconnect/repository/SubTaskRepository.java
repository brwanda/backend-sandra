package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

  /** All tasks in the chair's subcommittee */
  List<SubTask> findBySubcommitteeId(Long subcommitteeId);

  /** Tasks assigned to a specific member */
  List<SubTask> findByAssignedToId(Long memberId);

  /** Tasks linked to a specific resolution */
  List<SubTask> findByResolutionId(Long resolutionId);

  /** Tasks linked to a specific resolution scoped to a subcommittee */
  List<SubTask> findByResolutionIdAndSubcommitteeId(Long resolutionId, Long subcommitteeId);

  /** Tasks linked to a meeting */
  List<SubTask> findByMeetingId(Long meetingId);

  /** Tasks created by a specific chair */
  List<SubTask> findByAssignedByChairId(Long chairId);

  /** Tasks in a subcommittee filtered by status */
  List<SubTask> findBySubcommitteeIdAndStatus(Long subcommitteeId, SubTask.TaskStatus status);

  long countByAssignedToId(Long memberId);

  long countByAssignedToIdAndStatus(Long memberId, SubTask.TaskStatus status);

  List<SubTask> findTop5ByAssignedToIdOrderByCreatedAtDesc(Long memberId);

  /** Tasks assigned to a member ordered by creation date descending — used for consecutive rating check */
  List<SubTask> findByAssignedToIdOrderByCreatedAtDesc(Long memberId);

  /** Tasks requiring description (from TC meetings awaiting Committee Secretary input) */
  List<SubTask> findByRequiresDescriptionAndSubcommitteeId(Boolean requiresDescription, Long subcommitteeId);

  /** Tasks by source meeting type */
  List<SubTask> findBySourceMeetingType(String sourceMeetingType);

  /** Tasks requiring description across all subcommittees */
  List<SubTask> findByRequiresDescription(Boolean requiresDescription);
}
