package com.cells.backend.room;

import java.time.Instant;

/** A room as its member sees it. Screens show "churchName – roomName" with "Led by leaderName" below. */
public record MemberRoomResponse(
        Long roomId, String roomName,
        Long churchId, String churchName,
        String leaderName,
        Integer myParticipationNumber, Long memberCount,
        Instant joinedAt) {
}