package com.cells.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** How much EXP one submission earns. A fixed amount: EXP rewards taking part, never the quality of content. */
@ConfigurationProperties(prefix = "app.experience")
public record ExperienceProperties(int pointsPerSubmission) {

    public ExperienceProperties {
        if (pointsPerSubmission < 1) {
            throw new IllegalArgumentException("app.experience.points-per-submission must be at least 1");
        }
    }
}