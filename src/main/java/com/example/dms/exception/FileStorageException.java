package com.example.dms.exception;

/** Thrown when a MinIO operation (upload, download, or delete) fails. Maps to HTTP 500. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }
}
