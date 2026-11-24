package com.cells.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ContentReport, Long> {

    boolean existsBySubmissionIdAndReporterId(Long submissionId, Long reporterId);

    Optional<ContentReport> findByIdAndRoomId(Long id, Long roomId);

    // One query returns everything the list needs (no N+1). The reporter is not selected on purpose.
    @Query("""
            select new com.cells.backend.report.ReportResponse(
                r.id, s.id, s.devotionId, u.fullName, s.content, r.reason, r.note, r.createdAt, s.submittedAt)
            from ContentReport r
            join DevotionSubmission s on s.id = r.submissionId
            join User u on u.id = s.memberId
            where r.roomId = :roomId and r.status = com.cells.backend.report.ReportStatus.OPEN
            order by r.createdAt, r.id
            """)
    List<ReportResponse> findOpenForRoom(@Param("roomId") Long roomId);

    // Removing an answer closes every open report about it, not just the one the leader clicked.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ContentReport r set r.status = com.cells.backend.report.ReportStatus.REMOVED,
                r.resolvedAt = :now, r.resolvedBy = :by
            where r.submissionId = :submissionId and r.status = com.cells.backend.report.ReportStatus.OPEN
            """)
    int resolveOpenAsRemoved(@Param("submissionId") Long submissionId, @Param("now") Instant now, @Param("by") Long by);
}
