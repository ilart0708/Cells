package com.cells.backend.mail;

import com.cells.backend.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Sends one multipart (plain text + HTML) email. Callers decide what to do when it fails. */
@Component
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MailProperties properties;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(String to, String subject, String text, String html) throws MessagingException {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("No mail server is configured (spring.mail.host).");
        }
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(properties.fromOrDefault());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, html);
        sender.send(message);
    }
}
