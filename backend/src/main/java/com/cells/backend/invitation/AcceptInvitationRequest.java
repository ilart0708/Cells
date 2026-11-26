package com.cells.backend.invitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// acceptRules is optional on the server for now: the accept-invitation page always asks for it, but an older
// frontend that does not send it must still work. Make it required again once every client sends it.
public record AcceptInvitationRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(min = 12, max = 72, message = "Password must be 12 to 72 characters.") String password,
        Boolean acceptRules) {
}