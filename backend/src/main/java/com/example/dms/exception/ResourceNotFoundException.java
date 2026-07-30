package com.example.dms.exception;

/** Thrown when a requested resource does not exist or is not visible to the caller. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}