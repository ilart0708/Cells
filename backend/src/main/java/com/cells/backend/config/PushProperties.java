package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Phone push is off until the mobile app exists and a real PushSender (Firebase) is added. */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(boolean enabled) {
}
