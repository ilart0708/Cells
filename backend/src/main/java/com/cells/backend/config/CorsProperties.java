package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * The origins of the Android/iOS app: its pages are served from https://localhost (Android) or capacitor://localhost
 * (iOS). They may call the API directly and are the only origins that may use native sign-in mode. The website's own
 * origin is added to the CORS list separately (CorsConfig) from APP_FRONTEND_URL, and is deliberately not listed here.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public List<String> allowedOriginsOrEmpty() {
        return allowedOrigins == null ? List.of() : allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
