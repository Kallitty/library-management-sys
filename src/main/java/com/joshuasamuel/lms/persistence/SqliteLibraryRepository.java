package com.joshuasamuel.lms.persistence;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Stores the library in a single SQLite database file.
 *
 * <p>SQLite is a portable, file-based database: the whole library is one
 * {@code .db} file that can be copied between machines and opened with any
 * SQLite tool. Saves run inside a transaction, so a failed save rolls back and
 * leaves the previous state intact.
 *
 * <p>Needs the {@code org.xerial:sqlite-jdbc} driver on the classpath; Maven
 * adds it automatically.
 */
public final class SqliteLibraryRepository implements LibraryRepository {

    static final String DRIVER_CLASS = "org.sqlite.JDBC";

    private static final int SCHEMA_VERSION = 1;

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS books (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                title    TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                author   TEXT    NOT NULL,
                borrowed INTEGER NOT NULL DEFAULT 0 CHECK (borrowed IN (0, 1))
            )""";

    private static final String SELECT_ALL = "SELECT title, author, borrowed FROM books ORDER BY id";
    private static final String DELETE_ALL = "DELETE FROM books";
    private static final String INSERT = "INSERT INTO books (title, author, borrowed) VALUES (?, ?, ?)";

    private final Path file;

    public SqliteLibraryRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** True if the SQLite JDBC driver is on the classpath. */
    public static boolean isDriverAvailable() {
        try {
            Class.forName(DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<Book> load() {
        if (Files.notExists(file)) {
            return List.of();
        }
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(SELECT_ALL)) {
            List<Book> books = new ArrayList<>();
            while (rows.next()) {
                books.add(Book.restore(rows.getString("title"), rows.getString("author"), rows.getInt("borrowed") == 1));
            }
            return books;
        } catch (SQLException e) {
            throw new StorageException("Could not read the database " + file + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new StorageException("The database " + file + " contains an invalid book: " + e.getMessage(), e);
        }
    }

    @Override
    public void save(Collection<Book> books) {
        Objects.requireNonNull(books, "books");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (Statement delete = connection.createStatement();
                 PreparedStatement insert = connection.prepareStatement(INSERT)) {
                delete.executeUpdate(DELETE_ALL);
                for (Book book : books) {
                    insert.setString(1, book.getTitle());
                    insert.setString(2, book.getAuthor());
                    insert.setInt(3, book.isBorrowed() ? 1 : 0);
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new StorageException("Could not save the library to " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String describe() {
        return "SQLite database at " + file;
    }

    private Connection open() throws SQLException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new SQLException("Could not create the folder for " + file, e);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
