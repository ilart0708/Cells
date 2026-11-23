package com.cells.backend.auth;

import jakarta.validation.constraints.Size;

/** Only the native app sends this (the website uses its cookie). Optional on every endpoint that accepts it. */
public record RefreshRequest(@Size(max = 512) String refreshToken) {
}
