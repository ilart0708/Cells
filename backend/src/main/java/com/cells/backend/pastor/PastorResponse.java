package com.cells.backend.pastor;

import com.cells.backend.user.UserStatus;

import java.time.Instant;

public record PastorResponse(
        Long id, String fullName, String email, UserStatus status,
        Long churchId, String churchName, Instant createdAt) {
}