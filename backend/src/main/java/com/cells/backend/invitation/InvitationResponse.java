package com.cells.backend.invitation;

import com.cells.backend.room.Room;
import com.cells.backend.user.Role;

import java.time.Instant;

/** inviteUrl is filled only in the response to the create call; it is never stored. */
public record InvitationResponse(
        Long id, String email, String fullName, Role role,
        Long churchId, String churchName,
        Long roomId, String roomName,
        InvitationStatus status, Instant expiresAt, Instant createdAt,
        String inviteUrl) {

    public static InvitationResponse from(Invitation invitation, Instant now, String inviteUrl) {
        Room room = invitation.getRoom();
        return new InvitationResponse(
                invitation.getId(), invitation.getEmail(), invitation.getFullName(), invitation.getRole(),
                invitation.getChurch().getId(), invitation.getChurch().getName(),
                room == null ? null : room.getId(), room == null ? null : room.getName(),
                invitation.getStatus(now), invitation.getExpiresAt(), invitation.getCreatedAt(),
                inviteUrl);
    }
}