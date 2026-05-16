package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepo extends JpaRepository<Meeting, Long> {
    List<Meeting> findByCreatedById(Long createdById);

    List<Meeting> findByHostingCountryId(Long countryId);

    List<Meeting> findByCommitteeId(Long committeeId);

    List<Meeting> findBySubCommitteeId(Long subCommitteeId);

    List<Meeting> findByMeetingType(Meeting.MeetingType meetingType);

    List<Meeting> findByStatus(Meeting.MeetingStatus status);

    List<Meeting> findByMeetingDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Meeting> findByCreatedByIdAndStatus(Long createdById, Meeting.MeetingStatus status);

    long countByMeetingDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // --- Time-based filtered queries for Secretary scoping ---

    /** Meetings in future (upcoming) scoped by country */
    List<Meeting> findByHostingCountryIdAndMeetingDateAfter(Long countryId, LocalDateTime now);

    /** Meetings in past scoped by country */
    List<Meeting> findByHostingCountryIdAndMeetingDateBefore(Long countryId, LocalDateTime now);

    /** By subcommittee in future */
    List<Meeting> findBySubCommitteeIdAndMeetingDateAfter(Long subcommitteeId, LocalDateTime now);

    /** By subcommittee in past */
    List<Meeting> findBySubCommitteeIdAndMeetingDateBefore(Long subcommitteeId, LocalDateTime now);

    /** By committee in future */
    List<Meeting> findByCommitteeIdAndMeetingDateAfter(Long committeeId, LocalDateTime now);

    /** By committee in past */
    List<Meeting> findByCommitteeIdAndMeetingDateBefore(Long committeeId, LocalDateTime now);

    /** Meetings with at least one status, across country */
    @Query("SELECT m FROM Meeting m WHERE m.hostingCountry.id = :countryId AND m.status IN :statuses")
    List<Meeting> findByHostingCountryIdAndStatusIn(@Param("countryId") Long countryId,
            @Param("statuses") List<Meeting.MeetingStatus> statuses);

    /** Meetings with at least one status, across subcommittee */
    @Query("SELECT m FROM Meeting m WHERE m.subCommittee.id = :subcommitteeId AND m.status IN :statuses")
    List<Meeting> findBySubCommitteeIdAndStatusIn(@Param("subcommitteeId") Long subcommitteeId,
            @Param("statuses") List<Meeting.MeetingStatus> statuses);

    /** Meetings matching any of the given statuses (for scheduler) */
    List<Meeting> findByStatusIn(List<Meeting.MeetingStatus> statuses);

    /** Upcoming meetings (future, not cancelled) for a user's invitations */
    @Query("SELECT m FROM Meeting m JOIN m.invitations i WHERE i.user.id = :userId AND m.meetingDate > :now AND m.status NOT IN ('COMPLETED','CANCELLED','PENDING_MINUTES')")
    List<Meeting> findUpcomingByInvitedUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Archived/completed meetings for a user's invitations */
    @Query("SELECT m FROM Meeting m JOIN m.invitations i WHERE i.user.id = :userId AND (m.status = 'COMPLETED' OR m.status = 'PENDING_MINUTES' OR m.meetingDate < :now)")
    List<Meeting> findArchivedByInvitedUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Set createdBy to NULL for all meetings created by a specific user
     * This is used when deleting a user to maintain referential integrity
     */
    @Modifying
    @Query("UPDATE Meeting m SET m.createdBy = NULL WHERE m.createdBy.id = :userId")
    void nullifyCreatedByForUser(@Param("userId") Long userId);
}