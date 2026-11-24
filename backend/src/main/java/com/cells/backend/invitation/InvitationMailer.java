package com.cells.backend.invitation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Queues the registration email so it is sent only if the invitation was really saved. */
@Component
public class InvitationMailer {

    private final InvitationMailSender sender;

    public InvitationMailer(InvitationMailSender sender) {
        this.sender = sender;
    }

    public void queue(InvitationMail mail) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sender.send(mail);
                }
            });
        } else {
            sender.send(mail);
        }
    }
}
