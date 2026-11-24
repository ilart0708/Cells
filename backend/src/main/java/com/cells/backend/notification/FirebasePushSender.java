package com.cells.backend.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Sends phone pushes through Firebase Cloud Messaging. It never logs tokens or message text.
 * Only tokens Firebase calls permanently invalid are returned, so the dispatcher can forget them.
 * Any other failure throws, so the dispatcher retries the push (the email is not repeated).
 */
public class FirebasePushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    /** Must match NOTIFICATION_CHANNEL_ID in frontend/src/services/push.js. */
    static final String CHANNEL_ID = "cells-default";

    private final FirebaseMessaging messaging;

    public FirebasePushSender(String credentialsBase64) throws IOException {
        byte[] json = Base64.getMimeDecoder().decode(credentialsBase64.trim());
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json));
        FirebaseApp app = FirebaseApp.getApps().stream()
                .filter(a -> FirebaseApp.DEFAULT_APP_NAME.equals(a.getName()))
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(
                        FirebaseOptions.builder().setCredentials(credentials).build()));
        this.messaging = FirebaseMessaging.getInstance(app);
    }

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<String> list = List.copyOf(tokens);
        List<Message> messages = list.stream().map(token -> build(token, message)).toList();

        BatchResponse batch;
        try {
            batch = messaging.sendEach(messages);
        } catch (FirebaseMessagingException e) {
            throw new IllegalStateException("Firebase send failed: " + e.getMessagingErrorCode(), e);
        }

        List<String> invalid = new ArrayList<>();
        int failedForNow = 0;
        List<SendResponse> responses = batch.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }
            MessagingErrorCode code = response.getException() == null
                    ? null : response.getException().getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
                invalid.add(list.get(i));
            } else {
                failedForNow++;
                log.warn("Firebase could not deliver one push: {}", code);
            }
        }
        if (failedForNow > 0) {
            throw new IllegalStateException(failedForNow + " push message(s) could not be delivered; will retry.");
        }
        return invalid;
    }

    private static Message build(String token, PushMessage message) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                // Full name: this package also has its own class called Notification.
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder().setChannelId(CHANNEL_ID).build())
                        .build());
        if (message.data() != null) {
            builder.putAllData(message.data());
        }
        return builder.build();
    }
}