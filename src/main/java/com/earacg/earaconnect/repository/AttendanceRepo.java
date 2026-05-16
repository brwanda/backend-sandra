package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public interface AttendanceRepo extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByMeetingId(Long meetingId);
    List<Attendance> findByUserId(Long userId);
    Optional<Attendance> findByMeetingIdAndUserId(Long meetingId, Long userId);
    List<Attendance> findByMeetingIdAndStatus(Long meetingId, Attendance.AttendanceStatus status);
    List<Attendance> findByStatus(Attendance.AttendanceStatus status);

    /**
     * Delete all attendance records for a specific user
     */
    void deleteByUserId(Long userId);
}