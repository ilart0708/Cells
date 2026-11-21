package com.cells.backend.summary;

/**
 * The leader's view. summary and reflection are null until written. canWrite is false when the room is
 * archived or nobody has answered yet, and writeBlockedReason says why (null when canWrite is true).
 */
public record LeaderGuidanceResponse(
        Long devotionId, int answerCount, boolean canWrite, String writeBlockedReason,
        SummaryNoteResponse summary, SummaryNoteResponse reflection) {
}