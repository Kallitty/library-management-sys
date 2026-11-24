package com.joshuasamuel.lms.exception;

/** Thrown when no book in the library matches the given title. */
public class BookNotFoundException extends LibraryException {

    private static final long serialVersionUID = 1L;

    private final String title;

    public BookNotFoundException(String title) {
        super("No book titled \"" + title + "\" was found.");
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
