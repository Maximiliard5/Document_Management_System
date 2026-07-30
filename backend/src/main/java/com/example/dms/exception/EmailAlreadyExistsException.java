package com.example.dms.exception;

/** Thrown when registering with an email address that is already in use. Maps to HTTP 409. */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {

        super("Email already in use: " + email);
    }
}
