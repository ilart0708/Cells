package com.cells.backend.auth;

import com.cells.backend.user.UserResponse;

/** The login response for the native app: the same fields plus the refresh token, which it keeps in secure storage. */
public record NativeSessionResponse(
        String accessToken, String tokenType, long expiresIn, UserResponse user, String refreshToken) {

    public static NativeSessionResponse of(LoginResponse body, String refreshToken) {
        return new NativeSessionResponse(body.accessToken(), body.tokenType(), body.expiresIn(), body.user(), refreshToken);
    }
}
