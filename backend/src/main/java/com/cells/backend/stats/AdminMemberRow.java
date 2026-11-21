package com.cells.backend.stats;

import com.cells.backend.experience.LevelResponse;
import com.cells.backend.user.UserStatus;

import java.time.Instant;
import java.util.List;

/**
 * A member as the administrator sees them. rooms counts active rooms they are in. devotionsShared and
 * totalPoints cover every room they have ever shared in, so they stay when a member leaves a room or a room is archived.
 */
public record AdminMemberRow(
        Long memberId, String fullName, String email, UserStatus accountStatus, String churchName, Instant memberSince,
        int rooms, int devotionsShared, int totalPoints, LevelResponse level, List<MemberRoomToday> roomsToday) {
}