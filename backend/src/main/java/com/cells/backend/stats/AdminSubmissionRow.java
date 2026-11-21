package com.cells.backend.stats;

import java.time.Instant;
import java.time.LocalDate;

/** One thing a member shared: when, where and which passage. There is deliberately no text field. */
public record AdminSubmissionRow(
        Long submissionId, Instant submittedAt, LocalDate devotionDate,
        Long roomId, String roomName, String churchName,
        String reference, String title, int points) {
}