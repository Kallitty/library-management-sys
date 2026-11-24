package com.joshuasamuel.lms.exception;

/**
 * Thrown when a book is added with a title the library already holds.
 *
 * <p>Borrowing and returning work by title, so titles must be unique
 * (ignoring case and surrounding spaces) or those operations would be ambiguous.
 */
public class DuplicateBookException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public DuplicateBookException(String title) {
        super("A book titled \"" + title + "\" is already in the library.");
    }
}
