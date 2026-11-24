package com.cells.backend.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The password is asked again, so a stolen phone or an open browser cannot delete an account by accident. */
public record DeleteAccountRequest(@NotBlank @Size(max = 72) String password) {
}
