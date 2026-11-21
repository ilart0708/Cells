package com.cells.backend.stats;

/** One room's numbers. Screens show "churchName – roomName" with "Led by leaderName". */
public record RoomStatsResponse(
        Long roomId, String roomName, String churchName, Long leaderId, String leaderName,
        int members, int disabledMembers,
        TodayStats today, PeriodStats last7, PeriodStats last30) {
}