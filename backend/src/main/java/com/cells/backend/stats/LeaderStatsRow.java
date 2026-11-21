package com.cells.backend.stats;

import com.cells.backend.user.UserStatus;

/** One leader's numbers across their active rooms. members counts distinct people. */
public record LeaderStatsRow(
        Long leaderId, String fullName, String email, UserStatus accountStatus, String churchName,
        int rooms, int members, int disabledMembers,
        TodayStats today, PeriodStats last7, PeriodStats last30) {
}