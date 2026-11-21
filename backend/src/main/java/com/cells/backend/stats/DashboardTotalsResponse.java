package com.cells.backend.stats;

/** Totals over everything in scope. members counts distinct people. */
public record DashboardTotalsResponse(
        int leaders, int disabledLeaders, int rooms, int members, int disabledMembers,
        TodayStats today, PeriodStats last7, PeriodStats last30) {
}