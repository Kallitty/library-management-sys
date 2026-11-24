package com.joshuasamuel.lms.persistence;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.exception.StorageException;

import java.util.Collection;
import java.util.List;

/**
 * Saves and loads the library's books.
 *
 * <p>The rest of the app only depends on this interface, so the storage can be
 * swapped (CSV file, SQLite, or an in-memory fake in tests) without changing
 * the domain or the CLI.
 */
public interface LibraryRepository {

    /**
     * Loads every saved book. Returns an empty list if nothing has been saved yet.
     *
     * @throws StorageException if the storage exists but cannot be read or is corrupt
     */
    List<Book> load();

    /**
     * Replaces the saved state with the given books.
     * Either every book is saved or the previous state is kept; a failure never
     * leaves half a library behind.
     *
     * @throws StorageException if the books cannot be written
     */
    void save(Collection<Book> books);

    /** A short description for the user, e.g. {@code "SQLite database at data/library.db"}. */
    String describe();
}
