package com.cells.backend.common;

import java.util.Map;

/** Field-level problems found by business rules (for example "John has 21 chapters."). Answered with 400. */
public class InvalidFieldsException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidFieldsException(Map<String, String> errors) {
        super("Validation failed.");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}