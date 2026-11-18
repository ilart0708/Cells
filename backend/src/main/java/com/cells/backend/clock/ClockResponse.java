package com.cells.backend.clock;

import java.time.LocalDate;

/** The platform's own calendar day, and the last day a devotion can be prepared for. */
public record ClockResponse(LocalDate today, String zone, LocalDate latestDevotionDate) {
}