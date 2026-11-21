package com.cells.backend.history;

import java.time.LocalDate;
import java.util.List;

/** The totals add up every room the member has been in. weekStart is a Monday (Manila time). */
public record HistorySummaryResponse(
        LocalDate today, LocalDate weekStart, LocalDate monthStart,
        PeriodTotalsResponse thisWeek, PeriodTotalsResponse thisMonth, PeriodTotalsResponse allTime,
        List<RoomHistorySummaryResponse> rooms) {
}