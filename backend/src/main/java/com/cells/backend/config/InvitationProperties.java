package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.invitation")
public record InvitationProperties(Duration ttl, String baseUrl) {
}