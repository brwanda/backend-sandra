package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepo extends JpaRepository<Report, Long> {
    List<Report> findByResolutionId(Long resolutionId);

    List<Report> findBySubcommitteeId(Long subcommitteeId);

    List<Report> findBySubmittedById(Long submittedById);

    List<Report> findByStatus(Report.ReportStatus status);

    List<Report> findByReviewedByHodId(Long hodId);

    List<Report> findByReviewedByCommissionerId(Long commissionerId);

    List<Report> findByResolutionIdAndStatus(Long resolutionId, Report.ReportStatus status);

    List<Report> findBySubmittedAtAfter(LocalDateTime date);

    List<Report> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByStatus(Report.ReportStatus status);
}