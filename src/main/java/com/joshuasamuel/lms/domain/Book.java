package com.joshuasamuel.lms.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * A single book in the library.
 *
 * <p>Title and author are fixed once the book is created. Only the borrowed
 * flag changes, and only through {@link #borrowBook()} and {@link #returnBook()}.
 * Those two methods are package-private on purpose: the {@link Library} is the
 * one place that decides whether a loan is allowed, so code outside this
 * package cannot flip a book's state behind the library's back.
 */
public final class Book {

    private final String title;
    private final String author;
    private boolean isBorrowed;

    /**
     * Creates an available book.
     *
     * @throws IllegalArgumentException if the title or author is null or blank
     */
    public Book(String title, String author) {
        this(title, author, false);
    }

    private Book(String title, String author, boolean isBorrowed) {
        this.title = clean(title, "Title");
        this.author = clean(author, "Author");
        this.isBorrowed = isBorrowed;
    }

    /**
     * Rebuilds a book with a known loan state, for example when loading from storage.
     *
     * @throws IllegalArgumentException if the title or author is null or blank
     */
    public static Book restore(String title, String author, boolean isBorrowed) {
        return new Book(title, author, isBorrowed);
    }

    /**
     * Marks the book as borrowed.
     *
     * @return {@code true} if the book was available and is now borrowed,
     *         {@code false} if it was already borrowed (nothing changes)
     */
    boolean borrowBook() {
        if (isBorrowed) {
            return false;
        }
        isBorrowed = true;
        return true;
    }

    /**
     * Marks the book as returned.
     *
     * @return {@code true} if the book was borrowed and is now available,
     *         {@code false} if it was not borrowed (nothing changes)
     */
    boolean returnBook() {
        if (!isBorrowed) {
            return false;
        }
        isBorrowed = false;
        return true;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isBorrowed() {
        return isBorrowed;
    }

    /** True if this book's title matches the given one, ignoring case and extra spaces. */
    public boolean hasTitle(String otherTitle) {
        return otherTitle != null && normalise(title).equals(normalise(otherTitle));
    }

    /** Returns e.g. {@code "Dune by Frank Herbert (Available)"}. */
    @Override
    public String toString() {
        return title + " by " + author + " (" + (isBorrowed ? "Borrowed" : "Available") + ")";
    }

    /**
     * Two books are equal when they have the same title and author, ignoring case.
     * The loan state is left out because it changes over the book's life.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Book)) {
            return false;
        }
        Book that = (Book) other;
        return normalise(title).equals(normalise(that.title))
                && normalise(author).equals(normalise(that.author));
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalise(title), normalise(author));
    }

    /** Lower-cases and collapses whitespace so "  The  Hobbit " matches "the hobbit". */
    static String normalise(String text) {
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String clean(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be empty.");
        }
        return value.strip().replaceAll("\\s+", " ");
    }
}
