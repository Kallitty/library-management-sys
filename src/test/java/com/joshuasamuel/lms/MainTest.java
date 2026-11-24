package com.joshuasamuel.lms;

import com.joshuasamuel.lms.persistence.StorageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Main")
class MainTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

    private int run(String input, String... args) {
        return Main.run(args,
                new BufferedReader(new StringReader(input)),
                new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return outBuffer.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBuffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("defaults to SQLite storage")
    void defaultOptions() {
        Main.Options options = Main.Options.parse(new String[0]);

        assertEquals(StorageType.SQLITE, options.storage());
        assertNull(options.file());
        assertFalse(options.help());
    }

    @Test
    @DisplayName("parses storage and file options")
    void parsesOptions() {
        Main.Options options = Main.Options.parse(new String[]{"--storage=csv", "--file=books.csv"});

        assertEquals(StorageType.CSV, options.storage());
        assertEquals(Path.of("books.csv"), options.file());
    }

    @Test
    @DisplayName("rejects unknown options")
    void rejectsUnknownOption() {
        assertThrows(IllegalArgumentException.class, () -> Main.Options.parse(new String[]{"--colour=blue"}));
        assertEquals(2, run("", "--colour=blue"));
        assertTrue(err().contains("Unknown option: --colour=blue"), err());
    }

    @Test
    @DisplayName("--help prints usage and exits with 0")
    void help() {
        assertEquals(0, run("", "--help"));
        assertTrue(out().contains("Usage:"), out());
    }

    @Test
    @DisplayName("the library is still there after a restart")
    void persistsAcrossRestarts() {
        String file = "--file=" + tempDir.resolve("library.csv");

        assertEquals(0, run("1\nDune\nFrank Herbert\n5\nDune\n7\n", "--storage=csv", file));
        outBuffer.reset();
        assertEquals(0, run("3\n1\n7\n", "--storage=csv", file));

        assertTrue(out().contains("(1 book loaded)"), out());
        assertTrue(out().contains("1. Dune by Frank Herbert (Borrowed)"), out());
    }

    @Test
    @DisplayName("refuses to start, and leaves the file alone, if the data file is corrupt")
    void corruptFileStopsStartup() throws IOException {
        Path file = tempDir.resolve("library.csv");
        Files.writeString(file, "this is not a library file\n");

        assertEquals(1, run("7\n", "--storage=csv", "--file=" + file));

        assertTrue(err().contains("Could not load the library"), err());
        assertEquals("this is not a library file\n", Files.readString(file));
    }
}
