package com.cells.backend.room;

import java.time.Instant;

/**
 * name is the room's own name. Screens show "churchName – name" with "Led by leaderName" below,
 * as the product document describes. memberCount counts active members only.
 * pastorName is only filled for a church a leader registered by themselves (typed text);
 * churchVerified is false until an administrator verifies that church.
 */
public record RoomResponse(
        Long id, String name, RoomStatus status,
        Long churchId, String churchName,
        Long leaderId, String leaderName,
        Long memberCount,
        Instant createdAt, Instant archivedAt,
        String pastorName, boolean churchVerified) {
}