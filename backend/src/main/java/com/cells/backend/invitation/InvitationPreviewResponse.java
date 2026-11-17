package com.cells.backend.invitation;

import com.cells.backend.user.Role;

import java.time.Instant;

/** roomName and leaderName are filled only for member invitations. */
public record InvitationPreviewResponse(
        String email, String fullName, Role role, String churchName,
        String roomName, String leaderName, Instant expiresAt) {
}