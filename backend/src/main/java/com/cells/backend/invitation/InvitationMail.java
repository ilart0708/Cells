package com.cells.backend.invitation;

import com.cells.backend.user.Role;

import java.time.Instant;

/** Everything the invitation email needs. It holds the live link, so it is never stored or logged. */
public record InvitationMail(
        String to, String fullName, Role role, String churchName, String roomName,
        String inviterName, String inviteUrl, Instant expiresAt) {

    @Override
    public String toString() {
        return "InvitationMail[role=" + role + "]"; // keeps the link and the address out of any log line
    }
}
