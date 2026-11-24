package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Whether the app sends email at all, and the From header. The SMTP server itself (host, port, login) is
 * configured under spring.mail.*. With enabled=false nothing is sent and nothing fails.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(boolean enabled, String from) {

    public String fromOrDefault() {
        return (from == null || from.isBlank()) ? "CELLS <no-reply@cells.local>" : from;
    }
}
