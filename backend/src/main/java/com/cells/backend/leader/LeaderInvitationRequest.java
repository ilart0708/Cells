package com.cells.backend.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No churchId on purpose: a leader always joins the inviting pastor's own church. */
public record LeaderInvitationRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName) {
}