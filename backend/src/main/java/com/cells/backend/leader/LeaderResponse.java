package com.cells.backend.leader;

import com.cells.backend.user.UserStatus;

import java.time.Instant;

public record LeaderResponse(
        Long id, String fullName, String email, UserStatus status,
        Long churchId, String churchName,
        Long pastorId, String pastorName,
        Instant createdAt) {
}