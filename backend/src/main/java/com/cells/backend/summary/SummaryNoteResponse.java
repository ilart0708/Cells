package com.cells.backend.summary;

import java.time.Instant;

/** A note and when it was written. updatedAt later than createdAt means the leader edited it. */
public record SummaryNoteResponse(String content, Instant createdAt, Instant updatedAt) {

    public static SummaryNoteResponse from(DevotionSummary note) {
        return note == null ? null : new SummaryNoteResponse(note.getContent(), note.getCreatedAt(), note.getUpdatedAt());
    }
}