package com.cells.backend.auth;

import com.cells.backend.user.UserResponse;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}