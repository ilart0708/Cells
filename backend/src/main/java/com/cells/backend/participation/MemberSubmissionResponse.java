package com.cells.backend.participation;

import java.time.Instant;

/** A fellow member's answer: no email and no ids. participationNumber is null for someone who has left the room. */
public record MemberSubmissionResponse(
        Integer participationNumber, String fullName, String content, Instant submittedAt, boolean you) {
}