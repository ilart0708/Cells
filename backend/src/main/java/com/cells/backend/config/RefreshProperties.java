package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.refresh")
public record RefreshProperties(Duration ttl, boolean cookieSecure) {
}