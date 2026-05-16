package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.ReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportAttachmentRepo extends JpaRepository<ReportAttachment, Long> {
    List<ReportAttachment> findByReportIdOrderByUploadedAtDesc(Long reportId);
}
