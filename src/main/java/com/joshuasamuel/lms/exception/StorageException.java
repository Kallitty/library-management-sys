package com.joshuasamuel.lms.exception;

/** Thrown when the library cannot be loaded from, or saved to, its storage. */
public class StorageException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
