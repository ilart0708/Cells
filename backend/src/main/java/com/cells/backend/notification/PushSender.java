package com.cells.backend.notification;

import java.util.Collection;
import java.util.List;

/**
 * Delivers one push message to a user's phones. The real implementation (Firebase Cloud Messaging) is added
 * together with the mobile app; until then LoggingPushSender stands in and app.push.enabled stays false.
 */
public interface PushSender {

    /** Returns the tokens the push service rejected as permanently invalid, so they can be forgotten. */
    List<String> send(Collection<String> tokens, PushMessage message);
}
