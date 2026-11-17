package com.cells.backend.invitation;

import com.cells.backend.user.Role;

import java.time.Instant;

public record InvitationPreviewResponse(
        String email, String fullName, Role role, String churchName, Instant expiresAt) {
}