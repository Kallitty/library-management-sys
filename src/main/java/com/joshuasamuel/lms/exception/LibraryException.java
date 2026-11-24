package com.joshuasamuel.lms.exception;

/**
 * Base type for every error the library can raise.
 *
 * <p>It is unchecked so the domain API stays readable, but every subclass is
 * documented on the methods that throw it. The CLI catches this one type and
 * shows the message to the user, so a new error type never needs a new catch.
 */
public abstract class LibraryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected LibraryException(String message) {
        super(message);
    }

    protected LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
