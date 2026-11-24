package com.joshuasamuel.lms.domain;

import java.util.Comparator;

/** The orders in which a list of books can be shown. */
public enum SortOrder {

    /** A to Z by title, then by author for books with the same title. */
    TITLE(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER)),

    /** A to Z by author, then by title so each author's books are in order too. */
    AUTHOR(Comparator.comparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));

    private final Comparator<Book> comparator;

    SortOrder(Comparator<Book> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Book> comparator() {
        return comparator;
    }
}
