package com.earacg.earaconnect.service;

import com.earacg.earaconnect.model.Attendance;
import com.earacg.earaconnect.model.Meeting;
import com.earacg.earaconnect.repository.AttendanceRepo;
import com.earacg.earaconnect.repository.MeetingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private MeetingRepo meetingRepo;

    public List<Attendance> getAllAttendance() {
        return attendanceRepo.findAll();
    }

    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepo.findById(id);
    }

    public List<Attendance> getAttendanceByMeeting(Long meetingId) {
        return attendanceRepo.findByMeetingId(meetingId);
    }

    public List<Attendance> getAttendanceByUser(Long userId) {
        return attendanceRepo.findByUserId(userId);
    }

    public List<Attendance> getAttendanceByStatus(Attendance.AttendanceStatus status) {
        return attendanceRepo.findByStatus(status);
    }

    /**
     * Validates that the meeting is in a state that allows attendance recording.
     * Attendance remains open after minutes submission, so COMPLETED is also allowed.
     */
    public void validateMeetingForAttendance(Long meetingId) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found with id: " + meetingId));

        Meeting.MeetingStatus status = meeting.getStatus();
        if (status != Meeting.MeetingStatus.IN_PROGRESS
                && status != Meeting.MeetingStatus.PENDING_MINUTES
                && status != Meeting.MeetingStatus.COMPLETED) {
            throw new IllegalStateException(
                "Attendance can only be recorded for active or completed meetings. "
                + "Current meeting status: " + status + ". "
                + "Please ensure the meeting is not cancelled.");
        }
    }

    public Attendance createAttendance(Attendance attendance) {
        if (attendance.getMeeting() == null || attendance.getUser() == null) {
            return null;
        }

        // Validate meeting status before recording attendance
        validateMeetingForAttendance(attendance.getMeeting().getId());
        
        attendance.setRecordedAt(LocalDateTime.now());
        
        return attendanceRepo.save(attendance);
    }

    public Attendance updateAttendance(Long id, Attendance attendanceDetails) {
        Optional<Attendance> attendanceOpt = attendanceRepo.findById(id);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            
            if (attendanceDetails.getStatus() != null) {
                attendance.setStatus(attendanceDetails.getStatus());
            }
            
            if (attendanceDetails.getNotes() != null) {
                attendance.setNotes(attendanceDetails.getNotes());
            }
            
            if (attendanceDetails.getRecordedBy() != null) {
                attendance.setRecordedBy(attendanceDetails.getRecordedBy());
            }
            
            return attendanceRepo.save(attendance);
        }
        return null;
    }

    @Autowired
    private EntityHistoryService entityHistoryService;

    public boolean deleteAttendance(Long id) {
        java.util.Optional<Attendance> attOpt = attendanceRepo.findById(id);
        if (attOpt.isPresent()) {
            entityHistoryService.recordDeletion("Attendance", id, attOpt.get(), null, null);
            attendanceRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Attendance> createBulkAttendance(List<Attendance> attendanceRecords) {
        // Validate meeting status for all records
        if (!attendanceRecords.isEmpty() && attendanceRecords.get(0).getMeeting() != null) {
            validateMeetingForAttendance(attendanceRecords.get(0).getMeeting().getId());
        }

        for (Attendance record : attendanceRecords) {
            if (record.getRecordedAt() == null) {
                record.setRecordedAt(LocalDateTime.now());
            }
        }
        return attendanceRepo.saveAll(attendanceRecords);
    }
}
