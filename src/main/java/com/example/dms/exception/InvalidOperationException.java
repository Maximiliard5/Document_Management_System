package com.example.dms.exception;

/** Thrown when a request is syntactically valid but violates a business rule. Maps to HTTP 409. */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
