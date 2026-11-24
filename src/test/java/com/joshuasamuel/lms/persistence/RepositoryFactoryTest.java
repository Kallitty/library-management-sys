package com.joshuasamuel.lms.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("RepositoryFactory and StorageType")
class RepositoryFactoryTest {

    private static final Path DATA = Path.of("data");

    @Test
    @DisplayName("uses SQLite when requested and the driver is available")
    void sqliteWhenAvailable() {
        RepositoryFactory.Selection selection = RepositoryFactory.create(StorageType.SQLITE, DATA, null, true);

        assertInstanceOf(SqliteLibraryRepository.class, selection.repository());
        assertEquals("SQLite database at " + DATA.resolve("library.db"), selection.repository().describe());
        assertNull(selection.notice());
    }

    @Test
    @DisplayName("falls back to CSV, with a notice, when the SQLite driver is missing")
    void fallsBackToCsv() {
        RepositoryFactory.Selection selection = RepositoryFactory.create(StorageType.SQLITE, DATA, null, false);

        assertInstanceOf(CsvLibraryRepository.class, selection.repository());
        assertEquals("CSV file at " + DATA.resolve("library.csv"), selection.repository().describe());
        assertNotNull(selection.notice());
    }

    @Test
    @DisplayName("the fallback keeps a custom file name but switches it to .csv")
    void fallbackKeepsCustomName() {
        Path custom = Path.of("backups", "books.db");

        RepositoryFactory.Selection selection = RepositoryFactory.create(StorageType.SQLITE, DATA, custom, false);

        assertEquals("CSV file at " + Path.of("backups", "books.csv"), selection.repository().describe());
    }

    @Test
    @DisplayName("uses CSV when requested")
    void csvWhenRequested() {
        Path custom = Path.of("my-books.csv");

        RepositoryFactory.Selection selection = RepositoryFactory.create(StorageType.CSV, DATA, custom, true);

        assertInstanceOf(CsvLibraryRepository.class, selection.repository());
        assertEquals("CSV file at my-books.csv", selection.repository().describe());
        assertNull(selection.notice());
    }

    @Test
    @DisplayName("StorageType.parse accepts any case and surrounding spaces")
    void parseStorageType() {
        assertEquals(StorageType.SQLITE, StorageType.parse(" SQLite "));
        assertEquals(StorageType.CSV, StorageType.parse("csv"));
    }

    @Test
    @DisplayName("StorageType.parse rejects unknown names with a helpful message")
    void parseUnknown() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> StorageType.parse("mongo"));
        assertEquals("Unknown storage type \"mongo\". Use sqlite or csv.", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> StorageType.parse(null));
    }
}
