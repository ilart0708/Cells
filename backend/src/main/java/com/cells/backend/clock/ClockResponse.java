package com.cells.backend.clock;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The platform's own calendar day, the last day a devotion can be prepared for, and the moment the next daily
 * cycle starts (the next 12:00 AM in the platform's zone).
 */
public record ClockResponse(LocalDate today, String zone, LocalDate latestDevotionDate, Instant nextResetAt) {
}
