package com.joshuasamuel.lms.persistence;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CsvLibraryRepository")
class CsvLibraryRepositoryTest {

    @TempDir
    Path tempDir;

    private Path file;
    private CsvLibraryRepository repository;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("library.csv");
        repository = new CsvLibraryRepository(file);
    }

    private void write(String... lines) throws IOException {
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
    }

    private static String describe(List<Book> books) {
        return books.stream().map(Book::toString).toList().toString();
    }

    @Test
    @DisplayName("loads an empty library when no file exists yet")
    void missingFileIsEmpty() {
        assertTrue(repository.load().isEmpty());
    }

    @Test
    @DisplayName("saves and reloads books with their order and loan state")
    void roundTrip() {
        List<Book> books = List.of(
                Book.restore("Dune", "Frank Herbert", true),
                Book.restore("Emma", "Jane Austen", false));

        repository.save(books);
        List<Book> loaded = new CsvLibraryRepository(file).load();

        assertEquals(describe(books), describe(loaded));
    }

    @Test
    @DisplayName("keeps commas, quotes and non-English characters intact")
    void specialCharacters() {
        List<Book> books = List.of(
                Book.restore("Eats, Shoots & Leaves", "Lynne Truss", false),
                Book.restore("The \"Real\" Story", "O'Brien, Tim", true),
                Book.restore("Cien años de soledad", "Gabriel García Márquez", false));

        repository.save(books);

        assertEquals(describe(books), describe(repository.load()));
    }

    @Test
    @DisplayName("writes a readable CSV file with a header")
    void fileFormat() throws IOException {
        repository.save(List.of(Book.restore("Eats, Shoots & Leaves", "Lynne Truss", true)));

        assertEquals(List.of("title,author,borrowed", "\"Eats, Shoots & Leaves\",Lynne Truss,true"),
                Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("replaces the previous contents on each save")
    void saveReplaces() {
        repository.save(List.of(new Book("Dune", "Frank Herbert"), new Book("Emma", "Jane Austen")));
        repository.save(List.of(new Book("Emma", "Jane Austen")));

        assertEquals("[Emma by Jane Austen (Available)]", describe(repository.load()));
    }

    @Test
    @DisplayName("creates missing parent folders")
    void createsFolders() {
        Path nested = tempDir.resolve("a/b/library.csv");

        new CsvLibraryRepository(nested).save(List.of(new Book("Dune", "Frank Herbert")));

        assertTrue(Files.exists(nested));
    }

    @Test
    @DisplayName("leaves no temporary files behind")
    void noTempFiles() throws IOException {
        repository.save(List.of(new Book("Dune", "Frank Herbert")));

        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    @DisplayName("ignores blank lines and a byte-order mark")
    void toleratesBlankLinesAndBom() throws IOException {
        write("﻿title,author,borrowed", "Dune,Frank Herbert,false", "", "Emma,Jane Austen,true", "");

        assertEquals("[Dune by Frank Herbert (Available), Emma by Jane Austen (Borrowed)]",
                describe(repository.load()));
    }

    @Test
    @DisplayName("rejects a file with the wrong header")
    void wrongHeader() throws IOException {
        write("name,writer", "Dune,Frank Herbert");

        StorageException e = assertThrows(StorageException.class, repository::load);
        assertTrue(e.getMessage().contains("unexpected header"));
    }

    @Test
    @DisplayName("reports the line number of a row with the wrong number of fields")
    void wrongFieldCount() throws IOException {
        write(CsvLibraryRepository.HEADER, "Dune,Frank Herbert,false", "Emma,true");

        StorageException e = assertThrows(StorageException.class, repository::load);
        assertTrue(e.getMessage().contains("line 3"), e.getMessage());
        assertTrue(e.getMessage().contains("expected 3 fields but found 2"), e.getMessage());
    }

    @Test
    @DisplayName("rejects a borrowed value that is not true or false")
    void badBorrowedValue() throws IOException {
        write(CsvLibraryRepository.HEADER, "Dune,Frank Herbert,yes");

        StorageException e = assertThrows(StorageException.class, repository::load);
        assertTrue(e.getMessage().contains("borrowed must be true or false"), e.getMessage());
    }

    @Test
    @DisplayName("rejects an unclosed quoted field")
    void unclosedQuote() throws IOException {
        write(CsvLibraryRepository.HEADER, "\"Dune,Frank Herbert,false");

        StorageException e = assertThrows(StorageException.class, repository::load);
        assertTrue(e.getMessage().contains("not closed"), e.getMessage());
    }

    @Test
    @DisplayName("rejects a row with a blank title")
    void blankTitle() throws IOException {
        write(CsvLibraryRepository.HEADER, " ,Frank Herbert,false");

        StorageException e = assertThrows(StorageException.class, repository::load);
        assertTrue(e.getMessage().contains("Title must not be empty"), e.getMessage());
    }

    @Test
    @DisplayName("wraps write failures in a StorageException")
    void saveFailureIsReported() throws IOException {
        // A plain file sitting where the parent folder should be makes the save fail.
        Path blocked = tempDir.resolve("blocked");
        Files.writeString(blocked, "not a folder");
        CsvLibraryRepository broken = new CsvLibraryRepository(blocked.resolve("library.csv"));

        StorageException e = assertThrows(StorageException.class,
                () -> broken.save(List.of(new Book("Emma", "Jane Austen"))));
        assertTrue(e.getMessage().startsWith("Could not save the library"), e.getMessage());
        assertEquals("not a folder", Files.readString(blocked));
    }
}
