package com.cells.backend.stats;

/** One active room of a member. Screens show "churchName – roomName" with "Led by leaderName". */
public record MemberRoomToday(
        Long roomId, String roomName, String churchName, String leaderName,
        Integer participationNumber, TodayStatus today) {
}