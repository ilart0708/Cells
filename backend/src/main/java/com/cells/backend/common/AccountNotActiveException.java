package com.cells.backend.common;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException() {
        super("This account is not active. Please contact your administrator.");
    }
}