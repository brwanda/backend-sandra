package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResolutionRepo extends JpaRepository<Resolution, Long> {
    List<Resolution> findByMeetingId(Long meetingId);

    List<Resolution> findByCreatedById(Long createdById);

    List<Resolution> findByStatus(Resolution.ResolutionStatus status);

    List<Resolution> findByMeetingIdAndStatus(Long meetingId, Resolution.ResolutionStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * Set createdBy to NULL for all resolutions created by a specific user
     * This is used when deleting a user to maintain referential integrity
     */
    @Modifying
    @Query("UPDATE Resolution r SET r.createdBy = NULL WHERE r.createdBy.id = :userId")
    void nullifyCreatedByForUser(@Param("userId") Long userId);
}