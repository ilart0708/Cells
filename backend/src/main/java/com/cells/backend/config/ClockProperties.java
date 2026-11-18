package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clock")
public record ClockProperties(String zone) {
}