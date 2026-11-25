package com.cells.backend.invitation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(min = 12, max = 72, message = "Password must be 12 to 72 characters.") String password,
        @AssertTrue(message = "Please agree to the community rules to continue.") boolean acceptRules) {
}