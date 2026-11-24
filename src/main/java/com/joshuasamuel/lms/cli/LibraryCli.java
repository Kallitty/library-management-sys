package com.joshuasamuel.lms.cli;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.domain.Library;
import com.joshuasamuel.lms.domain.SortOrder;
import com.joshuasamuel.lms.exception.BookNotFoundException;
import com.joshuasamuel.lms.exception.LibraryException;
import com.joshuasamuel.lms.exception.StorageException;
import com.joshuasamuel.lms.persistence.LibraryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * The menu-driven console interface.
 *
 * <p>Input and output are passed in rather than read from {@code System.in} and
 * {@code System.out} directly, so tests can drive a whole session with a
 * scripted input and check what was printed.
 *
 * <p>Every change is saved straight away, so closing the window or a crash
 * loses nothing.
 */
public final class LibraryCli {

    private static final String MENU = """

            ===== Library Menu =====
            1. Add a new book
            2. View available books
            3. View all books
            4. Search for a book (title or author)
            5. Borrow a book
            6. Return a book
            7. Exit""";

    private final Library library;
    private final LibraryRepository repository;
    private final BufferedReader in;
    private final PrintStream out;

    public LibraryCli(Library library, LibraryRepository repository, BufferedReader in, PrintStream out) {
        this.library = Objects.requireNonNull(library, "library");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.in = Objects.requireNonNull(in, "in");
        this.out = Objects.requireNonNull(out, "out");
    }

    /** Shows the menu until the user chooses Exit or the input ends. */
    public void run() {
        out.println("Welcome to the Library Management System.");
        out.println("Storage: " + repository.describe() + " (" + count(library.size()) + " loaded)");

        boolean running = true;
        while (running) {
            out.println(MENU);
            String choice = prompt("Choose an option (1-7): ");
            if (choice == null) {
                break; // input closed, e.g. Ctrl+D or end of a piped file
            }
            running = handle(choice.strip());
        }
        out.println("Goodbye.");
    }

    /** Runs one menu choice. Returns false when the user wants to exit. */
    private boolean handle(String choice) {
        try {
            switch (choice) {
                case "1" -> addBook();
                case "2" -> viewAvailableBooks();
                case "3" -> viewAllBooks();
                case "4" -> search();
                case "5" -> borrowBook();
                case "6" -> returnBook();
                case "7" -> {
                    return false;
                }
                default -> out.println("\"" + choice + "\" is not an option. Please enter a number from 1 to 7.");
            }
        } catch (InputClosedException e) {
            return false;
        } catch (LibraryException | IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
        return true;
    }

    private void addBook() {
        String title = requireInput("Title: ");
        String author = requireInput("Author: ");
        Book book = new Book(title, author);
        library.addBook(book);
        out.println("Added: " + book);
        save();
    }

    private void viewAvailableBooks() {
        SortOrder order = askSortOrder();
        printBooks("Available books", library.viewAvailableBooks(order), "No books are available right now.");
    }

    private void viewAllBooks() {
        SortOrder order = askSortOrder();
        printBooks("All books", library.viewAllBooks(order), "The library has no books yet.");
    }

    private void search() {
        String query = requireInput("Enter a title or author (or part of one): ");
        printBooks("Books matching \"" + query.strip() + "\"", library.search(query), "No books match \"" + query.strip() + "\".");
    }

    private void borrowBook() {
        String title = requireInput("Title of the book to borrow: ");
        try {
            library.borrowBook(title);
        } catch (BookNotFoundException e) {
            suggestAlternatives(e, title);
            return;
        }
        out.println("You have borrowed \"" + library.searchBook(title).getTitle() + "\".");
        save();
    }

    private void returnBook() {
        String title = requireInput("Title of the book to return: ");
        try {
            library.returnBook(title);
        } catch (BookNotFoundException e) {
            suggestAlternatives(e, title);
            return;
        }
        out.println("Thank you for returning \"" + library.searchBook(title).getTitle() + "\".");
        save();
    }

    private SortOrder askSortOrder() {
        while (true) {
            String answer = prompt("Sort by (1) title or (2) author? [1]: ");
            if (answer == null) {
                throw new InputClosedException();
            }
            switch (answer.strip()) {
                case "", "1" -> {
                    return SortOrder.TITLE;
                }
                case "2" -> {
                    return SortOrder.AUTHOR;
                }
                default -> out.println("Please enter 1 or 2.");
            }
        }
    }

    private void printBooks(String heading, List<Book> books, String emptyMessage) {
        if (books.isEmpty()) {
            out.println(emptyMessage);
            return;
        }
        out.println(heading + " (" + books.size() + "):");
        for (int i = 0; i < books.size(); i++) {
            out.printf("  %d. %s%n", i + 1, books.get(i));
        }
    }

    /** Shows the not-found error, plus any partial matches as a hint. */
    private void suggestAlternatives(BookNotFoundException e, String title) {
        out.println("Error: " + e.getMessage());
        List<Book> similar = library.search(title);
        if (!similar.isEmpty()) {
            out.println("Did you mean:");
            similar.forEach(book -> out.println("  - " + book.getTitle()));
        }
    }

    /** Saves after a change. A failed save is reported but the change stays in memory. */
    private void save() {
        try {
            repository.save(library.viewAllBooks());
        } catch (StorageException e) {
            out.println("Warning: your change is kept for this session but could not be saved. " + e.getMessage());
        }
    }

    /** Asks until the user types something that is not blank. */
    private String requireInput(String message) {
        while (true) {
            String value = prompt(message);
            if (value == null) {
                throw new InputClosedException();
            }
            if (!value.isBlank()) {
                return value;
            }
            out.println("This field cannot be empty.");
        }
    }

    /** Prints the prompt and reads a line, or returns null if the input has ended. */
    private String prompt(String message) {
        out.print(message);
        out.flush();
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read input", e);
        }
    }

    private static String count(int books) {
        return books + (books == 1 ? " book" : " books");
    }

    /** Signals that the input stream ended part-way through a prompt. */
    private static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
