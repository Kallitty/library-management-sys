package com.joshuasamuel.lms.cli;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.domain.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drives whole CLI sessions with scripted input and checks the output. */
@DisplayName("LibraryCli")
class LibraryCliTest {

    private Library library;
    private InMemoryRepository repository;

    @BeforeEach
    void setUp() {
        library = new Library();
        repository = new InMemoryRepository();
    }

    /** Runs the CLI with the given lines as user input and returns everything it printed. */
    private String session(String... inputLines) {
        String input = String.join("\n", inputLines) + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new LibraryCli(library, repository, new BufferedReader(new StringReader(input)), out).run();

        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("adds a book, saves it and shows it in the list")
    void addAndView() {
        String output = session("1", "Dune", "Frank Herbert", "3", "1", "7");

        assertTrue(output.contains("Added: Dune by Frank Herbert (Available)"), output);
        assertTrue(output.contains("All books (1):"), output);
        assertTrue(output.contains("1. Dune by Frank Herbert (Available)"), output);
        assertEquals(List.of("Dune by Frank Herbert (Available)"), repository.saved());
        assertTrue(output.endsWith("Goodbye." + System.lineSeparator()), output);
    }

    @Test
    @DisplayName("asks again when a required field is left blank")
    void blankFieldReprompts() {
        String output = session("1", "   ", "Dune", "", "Frank Herbert", "7");

        assertTrue(output.contains("This field cannot be empty."), output);
        assertEquals(1, library.size());
    }

    @Test
    @DisplayName("reports a duplicate title without saving")
    void duplicateTitle() {
        library.addBook(new Book("Dune", "Frank Herbert"));

        String output = session("1", "DUNE", "Someone", "7");

        assertTrue(output.contains("Error: A book titled \"Dune\" is already in the library."), output);
        assertEquals(0, repository.saveCount());
    }

    @Test
    @DisplayName("borrows and returns a book, saving after each")
    void borrowAndReturn() {
        library.addBook(new Book("Dune", "Frank Herbert"));

        String output = session("5", "dune", "6", "Dune", "7");

        assertTrue(output.contains("You have borrowed \"Dune\"."), output);
        assertTrue(output.contains("Thank you for returning \"Dune\"."), output);
        assertEquals(2, repository.saveCount());
        assertFalse(library.searchBook("Dune").isBorrowed());
    }

    @Test
    @DisplayName("explains why a book cannot be borrowed or returned")
    void loanErrors() {
        library.addBook(new Book("Dune", "Frank Herbert"));

        String output = session("6", "Dune", "5", "Dune", "5", "Dune", "7");

        assertTrue(output.contains("Error: \"Dune\" cannot be returned because it is not borrowed."), output);
        assertTrue(output.contains("Error: \"Dune\" is already borrowed."), output);
        assertEquals(1, repository.saveCount());
    }

    @Test
    @DisplayName("suggests close matches when a title is not found")
    void suggestsAlternatives() {
        library.addBook(new Book("The Hobbit", "J.R.R. Tolkien"));

        String output = session("5", "Hobbit", "7");

        assertTrue(output.contains("Error: No book titled \"Hobbit\" was found."), output);
        assertTrue(output.contains("Did you mean:"), output);
        assertTrue(output.contains("- The Hobbit"), output);
    }

    @Test
    @DisplayName("searches by part of the author's name")
    void searchByAuthor() {
        library.addBook(new Book("Emma", "Jane Austen"));
        library.addBook(new Book("Dune", "Frank Herbert"));

        String output = session("4", "austen", "4", "nothing here", "7");

        assertTrue(output.contains("Books matching \"austen\" (1):"), output);
        assertTrue(output.contains("1. Emma by Jane Austen (Available)"), output);
        assertTrue(output.contains("No books match \"nothing here\"."), output);
    }

    @Test
    @DisplayName("lists available books sorted by author when asked")
    void availableSortedByAuthor() {
        library.addBook(new Book("Emma", "Jane Austen"));
        library.addBook(new Book("Dune", "Frank Herbert"));
        library.addBook(new Book("Beloved", "Toni Morrison"));
        library.borrowBook("Beloved");

        String output = session("2", "2", "7");

        int herbert = output.indexOf("1. Dune by Frank Herbert");
        int austen = output.indexOf("2. Emma by Jane Austen");
        assertTrue(herbert > 0 && austen > herbert, output);
        assertFalse(output.contains("Beloved"), output);
    }

    @Test
    @DisplayName("re-asks for the sort order after an invalid answer")
    void invalidSortChoice() {
        String output = session("3", "x", "1", "7");

        assertTrue(output.contains("Please enter 1 or 2."), output);
        assertTrue(output.contains("The library has no books yet."), output);
    }

    @Test
    @DisplayName("rejects an unknown menu option and keeps running")
    void unknownOption() {
        String output = session("42", "7");

        assertTrue(output.contains("\"42\" is not an option."), output);
        assertTrue(output.contains("Goodbye."), output);
    }

    @Test
    @DisplayName("keeps the change in memory and warns when saving fails")
    void saveFailure() {
        repository.failOnSave();

        String output = session("1", "Dune", "Frank Herbert", "7");

        assertTrue(output.contains("Warning: your change is kept for this session but could not be saved. Disk is full."), output);
        assertEquals(1, library.size());
    }

    @Test
    @DisplayName("exits cleanly if the input ends, even part-way through a prompt")
    void endOfInput() {
        String atMenu = session();
        String midPrompt = session("1", "Dune");

        assertTrue(atMenu.contains("Goodbye."), atMenu);
        assertTrue(midPrompt.contains("Goodbye."), midPrompt);
        assertEquals(0, library.size());
    }
}
