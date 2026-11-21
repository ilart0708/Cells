package com.cells.backend.history;

import com.cells.backend.room.RoomStatus;

/**
 * One room in the member's history. Screens show "churchName – roomName" with "Led by leaderName".
 * current is false once the member has left the room. roomStatus is ARCHIVED for archived rooms.
 */
public record RoomHistorySummaryResponse(
        Long roomId, String roomName, String churchName, String leaderName,
        RoomStatus roomStatus, boolean current,
        int currentStreak, int bestStreak,
        PeriodTotalsResponse thisWeek, PeriodTotalsResponse thisMonth, PeriodTotalsResponse allTime) {
}