package com.cells.backend.history;

import java.time.Instant;
import java.time.LocalDate;

/** One devotion in the member's history. points and submittedAt are set only when status is SHARED. */
public record HistoryEntryResponse(
        Long devotionId, LocalDate date, String reference, String title,
        HistoryStatus status, Integer points, Instant submittedAt) {
}