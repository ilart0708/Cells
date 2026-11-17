package com.cells.backend.invitation;

import com.cells.backend.user.Role;

import java.time.Instant;

/** inviteUrl is filled only in the response to the create call; it is never stored. */
public record InvitationResponse(
        Long id, String email, String fullName, Role role,
        Long churchId, String churchName,
        InvitationStatus status, Instant expiresAt, Instant createdAt,
        String inviteUrl) {

    public static InvitationResponse from(Invitation invitation, Instant now, String inviteUrl) {
        return new InvitationResponse(
                invitation.getId(), invitation.getEmail(), invitation.getFullName(), invitation.getRole(),
                invitation.getChurch().getId(), invitation.getChurch().getName(),
                invitation.getStatus(now), invitation.getExpiresAt(), invitation.getCreatedAt(),
                inviteUrl);
    }
}