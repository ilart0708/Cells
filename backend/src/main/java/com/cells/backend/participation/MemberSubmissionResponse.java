package com.cells.backend.participation;

import java.time.Instant;

/** A fellow member's answer: no email and no member ids (id is the answer's own id, used to report it). participationNumber is null for someone who has left the room. */
public record MemberSubmissionResponse(
        Long id, Integer participationNumber, String fullName, String content, Instant submittedAt, boolean you,
        boolean removed) {
}