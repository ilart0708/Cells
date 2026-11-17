package com.cells.backend.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No churchId and no roomId here: the room is in the URL, and the church comes from the room. */
public record MemberInvitationRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName) {
}