package com.example.dms.exception;

/** Thrown when creating a resource that conflicts with an existing one. Maps to HTTP 409. */
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
