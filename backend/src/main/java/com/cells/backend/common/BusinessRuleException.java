package com.cells.backend.common;

/** A well-formed request that breaks a product rule (answered with 422). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}