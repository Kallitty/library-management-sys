package com.joshuasamuel.lms.domain;

import com.joshuasamuel.lms.exception.BookAlreadyBorrowedException;
import com.joshuasamuel.lms.exception.BookNotBorrowedException;
import com.joshuasamuel.lms.exception.BookNotFoundException;
import com.joshuasamuel.lms.exception.DuplicateBookException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Manages the library's collection of books and enforces the loan rules.
 *
 * <p>This class knows nothing about files, databases or the console. That keeps
 * the rules easy to test and means the storage or the interface can change
 * without touching it.
 *
 * <p>Every list it hands out is a read-only snapshot, so callers cannot add or
 * remove books except through {@link #addBook(Book)}.
 */
public final class Library {

    private final ArrayList<Book> books = new ArrayList<>();

    /** Creates an empty library. */
    public Library() {
    }

    /**
     * Creates a library holding the given books, for example ones loaded from storage.
     *
     * @throws DuplicateBookException if two of the books share a title
     */
    public Library(Collection<Book> initialBooks) {
        Objects.requireNonNull(initialBooks, "initialBooks");
        initialBooks.forEach(this::addBook);
    }

    /**
     * Adds a new book to the collection.
     *
     * @throws NullPointerException   if {@code book} is null
     * @throws DuplicateBookException if a book with the same title is already held
     */
    public void addBook(Book book) {
        Objects.requireNonNull(book, "book");
        findBook(book.getTitle()).ifPresent(existing -> {
            throw new DuplicateBookException(existing.getTitle());
        });
        books.add(book);
    }

    /** Returns the books that are not borrowed, in the order they were added. */
    public List<Book> viewAvailableBooks() {
        return filter(book -> !book.isBorrowed());
    }

    /** Returns the books that are not borrowed, in the given order. */
    public List<Book> viewAvailableBooks(SortOrder order) {
        return sorted(viewAvailableBooks(), order);
    }

    /** Returns every book, in the order they were added. */
    public List<Book> viewAllBooks() {
        return List.copyOf(books);
    }

    /** Returns every book, in the given order. */
    public List<Book> viewAllBooks(SortOrder order) {
        return sorted(books, order);
    }

    /**
     * Finds a book by its exact title, ignoring case and extra spaces.
     *
     * @throws BookNotFoundException if no book has that title
     */
    public Book searchBook(String title) {
        return findBook(title).orElseThrow(() -> new BookNotFoundException(display(title)));
    }

    /** Like {@link #searchBook(String)}, but returns an empty Optional instead of throwing. */
    public Optional<Book> findBook(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        return books.stream().filter(book -> book.hasTitle(title)).findFirst();
    }

    /**
     * Finds every book whose title or author contains the query, ignoring case.
     * For example "tolk" finds books by J.R.R. Tolkien, and "ring" finds "The Lord of the Rings".
     *
     * @return matching books sorted by title, or an empty list for a blank query
     */
    public List<Book> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = Book.normalise(query);
        List<Book> matches = filter(book ->
                Book.normalise(book.getTitle()).contains(needle)
                        || Book.normalise(book.getAuthor()).contains(needle));
        return sorted(matches, SortOrder.TITLE);
    }

    /**
     * Lends out the book with the given title.
     *
     * <p>The boolean return keeps the method signature from the brief. It is always
     * {@code true} when the method returns normally; every failure is an exception
     * that says why, so a caller cannot silently ignore a failed loan.
     *
     * @throws BookNotFoundException        if no book has that title
     * @throws BookAlreadyBorrowedException if the book is already out on loan
     */
    public boolean borrowBook(String title) {
        Book book = searchBook(title);
        if (!book.borrowBook()) {
            throw new BookAlreadyBorrowedException(book.getTitle());
        }
        return true;
    }

    /**
     * Takes back the book with the given title.
     *
     * @return {@code true} when the return succeeds (see {@link #borrowBook(String)})
     * @throws BookNotFoundException    if no book has that title
     * @throws BookNotBorrowedException if the book was not borrowed
     */
    public boolean returnBook(String title) {
        Book book = searchBook(title);
        if (!book.returnBook()) {
            throw new BookNotBorrowedException(book.getTitle());
        }
        return true;
    }

    public int size() {
        return books.size();
    }

    public boolean isEmpty() {
        return books.isEmpty();
    }

    private List<Book> filter(Predicate<Book> condition) {
        return books.stream().filter(condition).toList();
    }

    private static List<Book> sorted(Collection<Book> source, SortOrder order) {
        Comparator<Book> comparator = Objects.requireNonNull(order, "order").comparator();
        return source.stream().sorted(comparator).toList();
    }

    private static String display(String title) {
        return title == null ? "" : title.strip();
    }
}
