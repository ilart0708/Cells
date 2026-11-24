package com.cells.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-origin rules for /api/**.
 *
 * Two kinds of origin are allowed. The app origins (app.cors.allowed-origins) are the Android/iOS app. The website's own
 * origin (taken from APP_FRONTEND_URL) must be allowed too: browsers add an Origin header to every same-origin POST, Netlify
 * forwards it to the backend, and Spring sees a request from "another" site (the Netlify address) arriving at the Render
 * address. Without it every website login and save is refused with 403 "Invalid CORS request". Only the app origins may
 * use native sign-in mode (see SessionResponseFactory), so allowing the website here gives it no extra power.
 * Credentials (cookies) are deliberately NOT allowed: the native app sends its
 * access token in the Authorization header and its refresh token in the request body, so no cookie ever crosses
 * origins and the SameSite=Strict refresh cookie of the website stays out of this.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties, InvitationProperties invitation) {
        List<String> origins = new ArrayList<>(properties.allowedOriginsOrEmpty());
        String website = originOf(invitation.baseUrl());
        if (website != null && !origins.contains(website)) {
            origins.add(website);
        }

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Client-Type"));
        // Lets the app show "wait N seconds" after too many login attempts.
        cors.setExposedHeaders(List.of("Retry-After"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    /** "https://site.example/some/path" becomes "https://site.example", the form an Origin header has. */
    static String originOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            return uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase() + port;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
