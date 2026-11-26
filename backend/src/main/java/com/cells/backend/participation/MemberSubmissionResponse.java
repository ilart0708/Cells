package com.cells.backend.participation;

import com.cells.backend.reaction.ReactionSummary;

import java.time.Instant;
import java.util.List;

/** A fellow member's answer: no email and no member ids (id is the answer's own id, used to report it). participationNumber is null for someone who has left the room. */
public record MemberSubmissionResponse(
        Long id, Integer participationNumber, String fullName, String content, String takeaway,
        List<HighlightRange> highlights, Instant submittedAt, boolean you, boolean removed,
        ReactionSummary reactions) {
}
