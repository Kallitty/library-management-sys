package com.joshuasamuel.lms.exception;

/** Thrown when someone tries to borrow a book that is already out on loan. */
public class BookAlreadyBorrowedException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BookAlreadyBorrowedException(String title) {
        super("\"" + title + "\" is already borrowed.");
    }
}
