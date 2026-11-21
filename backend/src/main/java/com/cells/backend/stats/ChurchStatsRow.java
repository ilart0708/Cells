package com.cells.backend.stats;

import com.cells.backend.church.ChurchStatus;

/** One church. pastors and leaders count people on the church's staff. members counts distinct people in its active rooms. */
public record ChurchStatsRow(
        Long churchId, String name, ChurchStatus status,
        int pastors, int leaders, int rooms, int members, int disabledMembers,
        TodayStats today, PeriodStats last7, PeriodStats last30) {
}