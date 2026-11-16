package com.cells.backend.auth;

public record AuthResult(LoginResponse body, String refreshToken) {
}