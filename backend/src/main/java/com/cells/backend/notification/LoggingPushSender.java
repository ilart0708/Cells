package com.cells.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/** Stand-in used until Firebase is wired in. It never logs tokens or message text. */
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        log.info("Push is enabled but no Firebase sender is installed: {} device(s) were not notified.", tokens.size());
        return List.of();
    }
}
