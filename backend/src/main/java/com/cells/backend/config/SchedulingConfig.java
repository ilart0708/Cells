package com.cells.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled (the midnight cycle job, the notification retry) and @Async (sending after a commit). */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
