package com.cells.backend.auth;

import com.cells.backend.config.RefreshProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieFactory {

    public static final String NAME = "cells_refresh";
    private static final String PATH = "/api/v1/auth";

    private final RefreshProperties properties;

    public RefreshCookieFactory(RefreshProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String value) {
        return build(value, properties.ttl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}