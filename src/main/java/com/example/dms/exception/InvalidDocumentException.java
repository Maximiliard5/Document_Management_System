package com.example.dms.exception;

/** Thrown when an uploaded file fails validation (empty, too large, disallowed type). Maps to HTTP 400. */
public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String message) {
        super(message);
    }
}
