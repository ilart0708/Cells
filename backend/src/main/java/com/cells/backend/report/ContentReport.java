package com.cells.backend.report;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "content_reports")
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    protected ContentReport() {
        // required by JPA
    }

    public ContentReport(Long submissionId, Long roomId, Long reporterId, ReportReason reason, String note) {
        this.submissionId = submissionId;
        this.roomId = roomId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public Long getRoomId() { return roomId; }
    public ReportStatus getStatus() { return status; }

    public void resolve(ReportStatus outcome, Instant now, Long byUserId) {
        this.status = outcome;
        this.resolvedAt = now;
        this.resolvedBy = byUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentReport other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
