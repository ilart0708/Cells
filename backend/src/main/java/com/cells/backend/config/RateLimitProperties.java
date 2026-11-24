package com.cells.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10") int maxRequests,
        @DefaultValue("1m") Duration window,
        String clientIpHeader,
        /** Temporary: logs the headers that may carry the visitor's address, to find out which one the host provides. */
        @DefaultValue("false") boolean debug) {
}