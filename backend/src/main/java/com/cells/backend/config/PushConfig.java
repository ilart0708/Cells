package com.cells.backend.config;

import com.cells.backend.notification.LoggingPushSender;
import com.cells.backend.notification.PushSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PushConfig {

    /** Replaced automatically when the app defines its own (Firebase) PushSender bean. */
    @Bean
    @ConditionalOnMissingBean(PushSender.class)
    public PushSender pushSender() {
        return new LoggingPushSender();
    }
}
