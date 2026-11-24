package com.cells.backend.report;

import java.time.Instant;

/** What the leader sees about one open report. The reporter is never named, so members can report safely. */
public record ReportResponse(
        Long reportId, Long submissionId, Long devotionId, String authorName, String content,
        ReportReason reason, String note, Instant reportedAt, Instant answerSubmittedAt) {
}
