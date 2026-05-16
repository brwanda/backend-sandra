package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.MeetingInvitation;
import com.earacg.earaconnect.service.MeetingInvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meeting-invitations")
public class MeetingInvitationController {

    @Autowired
    private MeetingInvitationService meetingInvitationService;

    @GetMapping
    public ResponseEntity<List<MeetingInvitation>> getAllInvitations() {
        try {
            List<MeetingInvitation> invitations = meetingInvitationService.getAllInvitations();
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            System.err.println("Error in getAllInvitations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingInvitation> getInvitationById(@PathVariable Long id) {
        return meetingInvitationService.getInvitationById(id)
                .map(invitation -> ResponseEntity.ok(invitation))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<List<MeetingInvitation>> getInvitationsByMeeting(@PathVariable Long meetingId) {
        try {
            List<MeetingInvitation> invitations = meetingInvitationService.getInvitationsByMeeting(meetingId);
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            System.err.println("Error in getInvitationsByMeeting: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MeetingInvitation>> getInvitationsByUser(@PathVariable Long userId) {
        try {
            List<MeetingInvitation> invitations = meetingInvitationService.getInvitationsByUser(userId);
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            System.err.println("Error in getInvitationsByUser: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MeetingInvitation>> getInvitationsByStatus(@PathVariable String status) {
        try {
            MeetingInvitation.InvitationStatus invitationStatus = MeetingInvitation.InvitationStatus
                    .valueOf(status.toUpperCase());
            List<MeetingInvitation> invitations = meetingInvitationService.getInvitationsByStatus(invitationStatus);
            return ResponseEntity.ok(invitations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error in getInvitationsByStatus: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<MeetingInvitation> createInvitation(@RequestBody MeetingInvitation invitation) {
        MeetingInvitation createdInvitation = meetingInvitationService.createInvitation(invitation);
        if (createdInvitation != null) {
            return ResponseEntity.ok(createdInvitation);
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingInvitation> updateInvitation(@PathVariable Long id,
            @RequestBody MeetingInvitation invitationDetails) {
        MeetingInvitation updatedInvitation = meetingInvitationService.updateInvitation(id, invitationDetails);
        if (updatedInvitation != null) {
            return ResponseEntity.ok(updatedInvitation);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteInvitation(@PathVariable Long id) {
        boolean deleted = meetingInvitationService.deleteInvitation(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Invitation deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/respond")
    public ResponseEntity<MeetingInvitation> respondToInvitation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String comment = request.get("comment");

            MeetingInvitation.InvitationStatus invitationStatus = MeetingInvitation.InvitationStatus
                    .valueOf(status.toUpperCase());

            MeetingInvitation updatedInvitation = meetingInvitationService.respondToInvitation(id, invitationStatus,
                    comment);
            if (updatedInvitation != null) {
                return ResponseEntity.ok(updatedInvitation);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error in respondToInvitation: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
