package com.joshuasamuel.lms.persistence;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.exception.StorageException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Stores the library in a plain CSV file with the header {@code title,author,borrowed}.
 *
 * <p>Needs nothing beyond the JDK, and the file can be opened in Excel or a text
 * editor. Fields containing commas or quotes are quoted as in RFC 4180.
 *
 * <p>Saving writes to a temporary file first and then moves it over the real
 * one, so a crash halfway through a save cannot corrupt the existing data.
 */
public final class CsvLibraryRepository implements LibraryRepository {

    static final String HEADER = "title,author,borrowed";

    private final Path file;

    public CsvLibraryRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    @Override
    public List<Book> load() {
        if (Files.notExists(file)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not read " + file + ": " + e.getMessage(), e);
        }

        List<Book> books = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            if (lineNumber == 1) {
                if (!HEADER.equals(stripBom(line).strip())) {
                    throw new StorageException(file + " is not a library file (unexpected header on line 1).");
                }
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            books.add(parseRow(line, lineNumber));
        }
        return books;
    }

    @Override
    public void save(Collection<Book> books) {
        Objects.requireNonNull(books, "books");
        Path temp = null;
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            temp = Files.createTempFile(parent, "library-", ".csv.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write(HEADER);
                writer.newLine();
                for (Book book : books) {
                    writer.write(escape(book.getTitle()) + "," + escape(book.getAuthor()) + "," + book.isBorrowed());
                    writer.newLine();
                }
            }
            moveIntoPlace(temp);
        } catch (IOException e) {
            throw new StorageException("Could not save the library to " + file + ": " + e.getMessage(), e);
        } finally {
            deleteQuietly(temp);
        }
    }

    @Override
    public String describe() {
        return "CSV file at " + file;
    }

    private Book parseRow(String line, int lineNumber) {
        List<String> fields = splitCsvLine(line, lineNumber);
        if (fields.size() != 3) {
            throw corrupt(lineNumber, "expected 3 fields but found " + fields.size());
        }
        String borrowed = fields.get(2).strip();
        if (!borrowed.equals("true") && !borrowed.equals("false")) {
            throw corrupt(lineNumber, "borrowed must be true or false, not \"" + borrowed + "\"");
        }
        try {
            return Book.restore(fields.get(0), fields.get(1), Boolean.parseBoolean(borrowed));
        } catch (IllegalArgumentException e) {
            throw corrupt(lineNumber, e.getMessage());
        }
    }

    /** Splits one CSV line, honouring double-quoted fields and doubled quotes inside them. */
    private List<String> splitCsvLine(String line, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw corrupt(lineNumber, "a quoted field is not closed");
        }
        fields.add(current.toString());
        return fields;
    }

    static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.startsWith(" ") || value.endsWith(" ")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private StorageException corrupt(int lineNumber, String reason) {
        return new StorageException(file + " line " + lineNumber + " is invalid: " + reason + ".");
    }

    private static String stripBom(String line) {
        return line.startsWith("﻿") ? line.substring(1) : line;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The temp file is left behind; harmless, and the save itself already succeeded or failed.
        }
    }
}
