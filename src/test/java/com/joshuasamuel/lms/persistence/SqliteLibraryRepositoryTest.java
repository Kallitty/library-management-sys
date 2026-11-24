package com.joshuasamuel.lms.persistence;

import com.joshuasamuel.lms.domain.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SqliteLibraryRepository")
class SqliteLibraryRepositoryTest {

    @TempDir
    Path tempDir;

    private Path file;
    private SqliteLibraryRepository repository;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("library.db");
        repository = new SqliteLibraryRepository(file);
    }

    private static String describe(List<Book> books) {
        return books.stream().map(Book::toString).toList().toString();
    }

    @Test
    @DisplayName("the SQLite driver is on the test classpath")
    void driverAvailable() {
        assertTrue(SqliteLibraryRepository.isDriverAvailable());
    }

    @Test
    @DisplayName("loads an empty library when no database exists yet")
    void missingFileIsEmpty() {
        assertTrue(repository.load().isEmpty());
    }

    @Test
    @DisplayName("saves and reloads books with their order and loan state")
    void roundTrip() {
        List<Book> books = List.of(
                Book.restore("Dune", "Frank Herbert", true),
                Book.restore("Eats, Shoots & Leaves", "Lynne Truss", false),
                Book.restore("Cien años de soledad", "Gabriel García Márquez", true));

        repository.save(books);

        assertEquals(describe(books), describe(repository.load()));
    }

    @Test
    @DisplayName("data survives a restart (a new repository on the same file)")
    void persistsAcrossInstances() {
        repository.save(List.of(Book.restore("Dune", "Frank Herbert", true)));

        List<Book> reloaded = new SqliteLibraryRepository(file).load();

        assertEquals("[Dune by Frank Herbert (Borrowed)]", describe(reloaded));
    }

    @Test
    @DisplayName("replaces the previous contents on each save")
    void saveReplaces() {
        repository.save(List.of(new Book("Dune", "Frank Herbert"), new Book("Emma", "Jane Austen")));
        repository.save(List.of(new Book("Emma", "Jane Austen")));

        assertEquals("[Emma by Jane Austen (Available)]", describe(repository.load()));
    }

    @Test
    @DisplayName("can save an empty library")
    void saveEmpty() {
        repository.save(List.of(new Book("Dune", "Frank Herbert")));
        repository.save(List.of());

        assertTrue(repository.load().isEmpty());
    }

    @Test
    @DisplayName("creates the database file and missing parent folders")
    void createsFile() {
        Path nested = tempDir.resolve("a/b/library.db");

        new SqliteLibraryRepository(nested).save(List.of(new Book("Dune", "Frank Herbert")));

        assertTrue(Files.exists(nested));
    }
}
