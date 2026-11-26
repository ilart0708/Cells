package com.cells.backend.participation;

import java.time.Instant;
import java.util.List;

/** removedAt is set when the leader removed the text after a report. participationNumber is null for someone who has since left the room (their answer stays in the history). */
public record LeaderSubmissionResponse(
        Long submissionId, Long memberId, String fullName, Integer participationNumber,
        String content, String takeaway, List<HighlightRange> highlights, Instant submittedAt, Instant removedAt) {

    /** Used by the query: the highlights come out of the database as text. */
    public LeaderSubmissionResponse(Long submissionId, Long memberId, String fullName, Integer participationNumber,
                                    String content, String takeaway, String highlightsText, Instant submittedAt,
                                    Instant removedAt) {
        this(submissionId, memberId, fullName, participationNumber, content, takeaway,
                Highlights.decode(highlightsText), submittedAt, removedAt);
    }
}
