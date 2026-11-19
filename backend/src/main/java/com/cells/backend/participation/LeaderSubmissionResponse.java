package com.cells.backend.participation;

import java.time.Instant;

/** participationNumber is null for someone who has since left the room (their answer stays in the history). */
public record LeaderSubmissionResponse(
        Long submissionId, Long memberId, String fullName, Integer participationNumber,
        String content, Instant submittedAt) {
}