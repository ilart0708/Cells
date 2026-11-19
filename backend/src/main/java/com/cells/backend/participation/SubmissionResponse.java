package com.cells.backend.participation;

import java.time.Instant;

public record SubmissionResponse(Long id, Long devotionId, String content, Instant submittedAt) {

    public static SubmissionResponse from(DevotionSubmission s) {
        return new SubmissionResponse(s.getId(), s.getDevotionId(), s.getContent(), s.getSubmittedAt());
    }
}