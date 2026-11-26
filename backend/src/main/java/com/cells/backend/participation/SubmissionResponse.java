package com.cells.backend.participation;

import java.time.Instant;
import java.util.List;

public record SubmissionResponse(
        Long id, Long devotionId, String content, String takeaway, List<HighlightRange> highlights,
        Instant submittedAt) {

    public static SubmissionResponse from(DevotionSubmission s) {
        return new SubmissionResponse(s.getId(), s.getDevotionId(), s.getContent(), s.getTakeaway(),
                Highlights.decode(s.getHighlights()), s.getSubmittedAt());
    }
}
