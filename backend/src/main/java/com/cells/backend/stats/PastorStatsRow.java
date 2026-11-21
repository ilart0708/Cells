package com.cells.backend.stats;

import com.cells.backend.user.UserStatus;

/** One pastor and the totals of their leaders' active rooms. */
public record PastorStatsRow(
        Long pastorId, String fullName, String email, UserStatus accountStatus, String churchName,
        int leaders, int rooms, int members, int disabledMembers,
        TodayStats today, PeriodStats last7, PeriodStats last30) {
}