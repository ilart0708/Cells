package com.cells.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** The one place that decides which timezone "today" is in. Everything else asks this Clock. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(ClockProperties properties) {
        // An invalid zone fails at startup instead of silently giving wrong dates.
        return Clock.system(ZoneId.of(properties.zone()));
    }
}