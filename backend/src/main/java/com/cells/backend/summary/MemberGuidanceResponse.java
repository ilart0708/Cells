package com.cells.backend.summary;

/**
 * The member's view. While unlocked is false, no text is sent, only lockedMessage. Once unlocked,
 * summary and reflection are each null if the leader has not written one.
 */
public record MemberGuidanceResponse(
        boolean unlocked, String lockedMessage, SummaryNoteResponse summary, SummaryNoteResponse reflection) {
}