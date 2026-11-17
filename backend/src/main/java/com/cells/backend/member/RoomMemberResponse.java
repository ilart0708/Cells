package com.cells.backend.room;

import com.cells.backend.user.UserStatus;

import java.time.Instant;

public record RoomMemberResponse(
        Long memberId, String fullName, String email, UserStatus accountStatus,
        Integer participationNumber, Instant joinedAt) {
}