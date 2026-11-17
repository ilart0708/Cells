package com.cells.backend.common;

public class InvalidInvitationException extends RuntimeException {
    public InvalidInvitationException() {
        super("This invitation link is invalid or has expired.");
    }
}