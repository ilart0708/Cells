package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Browser origins allowed to call the API directly (cross-origin). The website does not need this: Netlify
 * forwards /api to the backend, so the browser sees one origin. The Android/iOS app does: its pages are served
 * from https://localhost (Android) or capacitor://localhost (iOS).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public List<String> allowedOriginsOrEmpty() {
        return allowedOrigins == null ? List.of() : allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
