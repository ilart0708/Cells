package com.cells.backend.invitation;

import com.cells.backend.config.MailProperties;
import com.cells.backend.mail.EmailLayout;
import com.cells.backend.mail.EmailSender;
import com.cells.backend.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes and sends the registration email on a background thread. The link only exists in memory (the database
 * keeps just its hash), so there is no retry queue: if sending fails the inviter still has the copyable link
 * in the app, and inviting again sends a fresh one.
 */
@Component
public class InvitationMailSender {

    private static final Logger log = LoggerFactory.getLogger(InvitationMailSender.class);

    private final EmailSender emailSender;
    private final MailProperties properties;
    private final Clock clock;

    public InvitationMailSender(EmailSender emailSender, MailProperties properties, Clock clock) {
        this.emailSender = emailSender;
        this.properties = properties;
        this.clock = clock;
    }

    @Async
    public void send(InvitationMail mail) {
        if (!properties.enabled()) {
            return;
        }
        try {
            String expires = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
                    .withZone(clock.getZone()).format(mail.expiresAt());
            String roleLabel = roleLabel(mail.role());

            List<String> paragraphs = new ArrayList<>();
            paragraphs.add("Hello " + mail.fullName() + ",");
            String where = mail.roomName() == null
                    ? mail.churchName()
                    : mail.churchName() + " – " + mail.roomName();
            paragraphs.add(mail.inviterName() + " invited you to join " + where + " on CELLS as "
                    + article(roleLabel) + " " + roleLabel + ".");
            paragraphs.add("Use the button below to create your account and choose your own password. "
                    + "The link works once and expires on " + expires + ".");

            String heading = "You’re invited to CELLS";
            String button = "Create your account";
            String footnote = "If you weren’t expecting this invitation, you can ignore this email.";

            emailSender.send(mail.to(), "You’re invited to join " + mail.churchName() + " on CELLS",
                    EmailLayout.text(heading, paragraphs, button, mail.inviteUrl(), footnote),
                    EmailLayout.html(heading, paragraphs, button, mail.inviteUrl(), footnote));
            log.info("Invitation email sent ({}).", mail.role());
        } catch (Exception e) {
            log.warn("Invitation email ({}) could not be sent: {}. The invite link can still be copied in the app.",
                    mail.role(), e.getClass().getSimpleName());
        }
    }

    private static String roleLabel(Role role) {
        return switch (role) {
            case PASTOR -> "Pastor";
            case LEADER -> "Cell Group Leader";
            case MEMBER -> "Member";
            case ADMIN -> "Administrator";
        };
    }

    private static String article(String label) {
        return "AEIOU".indexOf(label.charAt(0)) >= 0 ? "an" : "a";
    }
}
