package com.cells.backend.notification;

import com.cells.backend.config.InvitationProperties;
import com.cells.backend.config.MailProperties;
import com.cells.backend.config.PushProperties;
import com.cells.backend.mail.EmailLayout;
import com.cells.backend.mail.EmailSender;
import com.cells.backend.notification.NotificationMessages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends queued notifications by email and push. It runs right after a transaction commits (so a mail
 * server problem can never undo a submission or an invitation) and again every few minutes to retry.
 *
 * Each row is "claimed" with a conditional UPDATE first, so two runs (or two servers) never send the same
 * notification twice, and each channel remembers whether it already succeeded so a retry repeats only
 * the failed one.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 100;
    private static final Duration STALE_AFTER = Duration.ofMinutes(10);
    private static final List<NotificationStatus> RETRYABLE = List.of(NotificationStatus.PENDING, NotificationStatus.FAILED);

    private final NotificationRepository repository;
    private final DeviceTokenRepository deviceTokens;
    private final EmailSender emailSender;
    private final PushSender pushSender;
    private final MailProperties mailProperties;
    private final PushProperties pushProperties;
    private final InvitationProperties invitationProperties;
    private final Clock clock;

    public NotificationDispatcher(NotificationRepository repository,
                                  DeviceTokenRepository deviceTokens,
                                  EmailSender emailSender,
                                  PushSender pushSender,
                                  MailProperties mailProperties,
                                  PushProperties pushProperties,
                                  InvitationProperties invitationProperties,
                                  Clock clock) {
        this.repository = repository;
        this.deviceTokens = deviceTokens;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.mailProperties = mailProperties;
        this.pushProperties = pushProperties;
        this.invitationProperties = invitationProperties;
        this.clock = clock;
    }

    /** Called after a commit: runs on its own thread, so the request that queued the message is not slowed down. */
    @Async
    public void dispatchAsync() {
        dispatchPending();
    }

    /** The safety net: picks up anything that failed, was left pending, or was interrupted by a restart. */
    @Scheduled(fixedDelayString = "${app.notification.retry-delay:PT2M}",
            initialDelayString = "${app.notification.initial-delay:PT30S}")
    public void dispatchPending() {
        try {
            Instant now = clock.instant();
            Instant stale = now.minus(STALE_AFTER);
            List<Long> ids = repository.findDispatchable(
                    RETRYABLE, NotificationStatus.SENDING, stale, MAX_ATTEMPTS, PageRequest.of(0, BATCH_SIZE));
            for (Long id : ids) {
                if (repository.claim(id, NotificationStatus.SENDING, RETRYABLE, now, stale, MAX_ATTEMPTS) == 1) {
                    deliver(id);
                }
            }
        } catch (RuntimeException e) {
            // Never let a failure here stop the scheduler thread.
            log.warn("Notification dispatch run failed: {}", e.toString());
        }
    }

    private void deliver(Long id) {
        NotificationDetails details = repository.findDetails(id).orElse(null);
        if (details == null) {
            repository.finish(id, NotificationStatus.FAILED, "The room, devotion or user no longer exists.", clock.instant());
            return;
        }

        Message message = NotificationMessages.of(details);
        String link = linkTo(message.path());
        List<String> errors = new ArrayList<>();

        // ---- email ----
        boolean emailDone = details.emailSentAt() != null || !mailProperties.enabled();
        if (!emailDone) {
            try {
                String footnote = "You’re receiving this because you belong to " + details.roomLabel() + " on CELLS.";
                emailSender.send(details.email(), message.subject(),
                        EmailLayout.text(message.heading(), message.paragraphs(), message.buttonLabel(), link, footnote),
                        EmailLayout.html(message.heading(), message.paragraphs(), message.buttonLabel(), link, footnote));
                repository.markEmailSent(id, clock.instant());
                emailDone = true;
            } catch (Exception e) {
                errors.add("email: " + e.getClass().getSimpleName());
                log.warn("Email for notification {} failed: {}", id, e.toString());
            }
        }

        // ---- phone push ----
        boolean pushDone = details.pushSentAt() != null || !pushProperties.enabled();
        if (!pushDone) {
            try {
                List<String> tokens = deviceTokens.findTokensByUserId(details.userId());
                if (!tokens.isEmpty()) {
                    List<String> invalid = pushSender.send(tokens, message.push());
                    if (!invalid.isEmpty()) {
                        deviceTokens.deleteTokens(invalid);
                    }
                    repository.markPushSent(id, clock.instant());
                }
                pushDone = true; // no phone registered means there is nothing to wait for
            } catch (Exception e) {
                errors.add("push: " + e.getClass().getSimpleName());
                log.warn("Push for notification {} failed: {}", id, e.toString());
            }
        }

        if (emailDone && pushDone) {
            repository.finish(id, NotificationStatus.SENT, null, clock.instant());
        } else {
            repository.finish(id, NotificationStatus.FAILED, truncate(String.join("; ", errors)), clock.instant());
        }
    }

    private String linkTo(String path) {
        return invitationProperties.baseUrl().replaceAll("/+$", "") + path;
    }

    private static String truncate(String value) {
        return value.length() <= 300 ? value : value.substring(0, 300);
    }
}
