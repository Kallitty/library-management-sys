package com.joshuasamuel.lms.exception;

/** Thrown when someone tries to return a book that was never borrowed. */
public class BookNotBorrowedException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BookNotBorrowedException(String title) {
        super("\"" + title + "\" cannot be returned because it is not borrowed.");
    }
}
