package com.joshuasamuel.lms.persistence;

import java.nio.file.Path;
import java.util.Locale;

/** The storage formats the app can use, each with a default file name. */
public enum StorageType {

    SQLITE("library.db"),
    CSV("library.csv");

    private final String defaultFileName;

    StorageType(String defaultFileName) {
        this.defaultFileName = defaultFileName;
    }

    public Path defaultFile(Path dataDirectory) {
        return dataDirectory.resolve(defaultFileName);
    }

    /**
     * Parses a name such as {@code "sqlite"} or {@code "CSV"}.
     *
     * @throws IllegalArgumentException for an unknown name
     */
    public static StorageType parse(String name) {
        try {
            return valueOf(name.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown storage type \"" + name + "\". Use sqlite or csv.", e);
        }
    }
}
