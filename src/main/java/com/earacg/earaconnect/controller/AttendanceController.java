package com.earacg.earaconnect.controller;

import com.earacg.earaconnect.model.Attendance;
import com.earacg.earaconnect.model.CSubCommitteeMembers;
import com.earacg.earaconnect.model.Meeting;
import com.earacg.earaconnect.model.User;
import com.earacg.earaconnect.repository.CSubCommitteeMembersRepo;
import com.earacg.earaconnect.repository.MeetingRepo;
import com.earacg.earaconnect.repository.UserRepo;
import com.earacg.earaconnect.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CSubCommitteeMembersRepo cSubCommitteeMembersRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private MeetingRepo meetingRepo;

    @GetMapping
    public ResponseEntity<List<Attendance>> getAllAttendance() {
        try {
            List<Attendance> attendance = attendanceService.getAllAttendance();
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            System.err.println("Error in getAllAttendance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attendance> getAttendanceById(@PathVariable Long id) {
        return attendanceService.getAttendanceById(id)
                .map(attendance -> ResponseEntity.ok(attendance))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<List<Attendance>> getAttendanceByMeeting(@PathVariable Long meetingId) {
        try {
            List<Attendance> attendance = attendanceService.getAttendanceByMeeting(meetingId);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            System.err.println("Error in getAttendanceByMeeting: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Attendance>> getAttendanceByUser(@PathVariable Long userId) {
        try {
            List<Attendance> attendance = attendanceService.getAttendanceByUser(userId);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            System.err.println("Error in getAttendanceByUser: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Attendance>> getAttendanceByStatus(@PathVariable String status) {
        try {
            Attendance.AttendanceStatus attendanceStatus = Attendance.AttendanceStatus.valueOf(status.toUpperCase());
            List<Attendance> attendance = attendanceService.getAttendanceByStatus(attendanceStatus);
            return ResponseEntity.ok(attendance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error in getAttendanceByStatus: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Attendance> createAttendance(@RequestBody Attendance attendance) {
        Attendance createdAttendance = attendanceService.createAttendance(attendance);
        if (createdAttendance != null) {
            return ResponseEntity.ok(createdAttendance);
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attendance> updateAttendance(@PathVariable Long id,
            @RequestBody Attendance attendanceDetails) {
        Attendance updatedAttendance = attendanceService.updateAttendance(id, attendanceDetails);
        if (updatedAttendance != null) {
            return ResponseEntity.ok(updatedAttendance);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAttendance(@PathVariable Long id) {
        boolean deleted = attendanceService.deleteAttendance(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Attendance record deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Attendance>> createBulkAttendance(@RequestBody List<Attendance> attendanceRecords) {
        try {
            List<Attendance> createdRecords = attendanceService.createBulkAttendance(attendanceRecords);
            return ResponseEntity.ok(createdRecords);
        } catch (Exception e) {
            System.err.println("Error in createBulkAttendance: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Bulk save attendance by CSubCommitteeMembers IDs.
     * Resolves each member to a User by matching email, then creates attendance records.
     * Payload: { meetingId: Long, recordedById: Long, records: [{ memberId: Long, status: String, notes: String }] }
     */
    @PostMapping("/bulk-by-members")
    public ResponseEntity<?> createBulkAttendanceByMembers(@RequestBody Map<String, Object> payload) {
        try {
            Long meetingId = Long.valueOf(payload.get("meetingId").toString());
            Long recordedById = payload.containsKey("recordedById") && payload.get("recordedById") != null
                    ? Long.valueOf(payload.get("recordedById").toString()) : null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) payload.get("records");

            if (records == null || records.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No attendance records provided"));
            }

            Meeting meeting = meetingRepo.findById(meetingId).orElse(null);
            if (meeting == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Meeting not found with id: " + meetingId));
            }

            // Attendance remains editable even after minutes are submitted.
            if (meeting.getStatus() != Meeting.MeetingStatus.IN_PROGRESS
                    && meeting.getStatus() != Meeting.MeetingStatus.PENDING_MINUTES
                    && meeting.getStatus() != Meeting.MeetingStatus.COMPLETED) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Attendance can only be recorded for active or completed meetings. Current status: " + meeting.getStatus()
                ));
            }

            User recordedBy = recordedById != null ? userRepo.findById(recordedById).orElse(null) : null;

            List<Attendance> attendanceList = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            for (Map<String, Object> record : records) {
                Long memberId = Long.valueOf(record.get("memberId").toString());
                String status = record.getOrDefault("status", "PRESENT").toString();
                String notes = record.getOrDefault("notes", "").toString();

                // Find the CSubCommitteeMembers entity
                CSubCommitteeMembers member = cSubCommitteeMembersRepo.findById(memberId).orElse(null);
                if (member == null) {
                    warnings.add("Member not found with id: " + memberId);
                    continue;
                }

                // Resolve to User by email
                User user = null;
                if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                    user = userRepo.findByEmailIgnoreCase(member.getEmail()).orElse(null);
                }

                if (user == null) {
                    // Try matching by name as fallback
                    warnings.add("No user account found for member: " + member.getName() + " (email: " + member.getEmail() + "). Skipping.");
                    continue;
                }

                Attendance attendance = new Attendance();
                attendance.setMeeting(meeting);
                attendance.setUser(user);
                attendance.setStatus(Attendance.AttendanceStatus.valueOf(status));
                attendance.setNotes(notes);
                attendance.setRecordedBy(recordedBy);
                attendance.setRecordedAt(LocalDateTime.now());

                attendanceList.add(attendance);
            }

            if (attendanceList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No valid attendance records could be created",
                        "warnings", warnings
                ));
            }

            List<Attendance> saved = attendanceService.createBulkAttendance(attendanceList);

            Map<String, Object> response = new HashMap<>();
            response.put("attendance", saved);
            response.put("savedCount", saved.size());
            if (!warnings.isEmpty()) {
                response.put("warnings", warnings);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in createBulkAttendanceByMembers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save attendance: " + e.getMessage()));
        }
    }
}
