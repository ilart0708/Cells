package com.cells.backend.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A leader signing up without an invitation. churchName and pastorName are typed text: the church is
 * created as "not verified", and nothing is linked to a pastor account.
 */
public record RegisterLeaderRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 12, max = 72, message = "Password must be 12 to 72 characters.") String password,
        @NotBlank @Size(max = 150) String churchName,
        @NotBlank @Size(max = 150) String pastorName,
        @Email @Size(max = 255) String pastorEmail,
        @AssertTrue(message = "You must agree to the community rules.") boolean acceptRules) {
}
