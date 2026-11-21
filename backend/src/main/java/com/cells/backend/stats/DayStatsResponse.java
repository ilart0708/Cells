package com.cells.backend.stats;

import java.time.LocalDate;

/** One past devotion day of a room. expected = members present that day minus those exempt. */
public record DayStatsResponse(
        LocalDate date, String reference, String title, int expected, int shared, Integer ratePercent) {
}