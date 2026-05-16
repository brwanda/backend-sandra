package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.MeetingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingInvitationRepo extends JpaRepository<MeetingInvitation, Long> {
    
    /**
     * Find all invitations for a specific meeting
     */
    List<MeetingInvitation> findByMeetingId(Long meetingId);
    
    /**
     * Find all invitations for a specific user
     */
    List<MeetingInvitation> findByUserId(Long userId);
    
    /**
     * Find invitation for a specific meeting and user combination
     */
    Optional<MeetingInvitation> findByMeetingIdAndUserId(Long meetingId, Long userId);
    
    /**
     * Find invitations by status
     */
    List<MeetingInvitation> findByStatus(MeetingInvitation.InvitationStatus status);
    
    /**
     * Find invitations for a meeting with specific status
     */
    List<MeetingInvitation> findByMeetingIdAndStatus(Long meetingId, MeetingInvitation.InvitationStatus status);
    
    /**
     * Count invitations by status for a user
     */
    long countByUserIdAndStatus(Long userId, MeetingInvitation.InvitationStatus status);
    
    /**
     * Find pending invitations for a user
     */
    List<MeetingInvitation> findByUserIdAndStatus(Long userId, MeetingInvitation.InvitationStatus status);

    long countByMeetingIdAndStatus(Long meetingId, MeetingInvitation.InvitationStatus status);

    /**
     * Delete all invitations for a specific user
     */
    void deleteByUserId(Long userId);
}
