package com.cells.backend.config;

import com.cells.backend.notification.FirebasePushSender;
import com.cells.backend.notification.LoggingPushSender;
import com.cells.backend.notification.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    /**
     * Uses Firebase when FIREBASE_CREDENTIALS_BASE64 is set. If it is missing, or cannot be read, the app still
     * starts and pushes are only logged. Whether pushes are sent at all is still decided by app.push.enabled.
     */
    @Bean
    public PushSender pushSender(@Value("${FIREBASE_CREDENTIALS_BASE64:}") String credentials) {
        if (credentials == null || credentials.isBlank()) {
            return new LoggingPushSender();
        }
        try {
            PushSender sender = new FirebasePushSender(credentials);
            log.info("Firebase push sender is ready.");
            return sender;
        } catch (Exception e) {
            log.error("Firebase credentials could not be loaded; push stays on the logging stand-in: {}", e.toString());
            return new LoggingPushSender();
        }
    }
}