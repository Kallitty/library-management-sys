package com.joshuasamuel.lms.persistence;

import java.nio.file.Path;
import java.util.Objects;

/** Picks the right {@link LibraryRepository} for the chosen storage type. */
public final class RepositoryFactory {

    private RepositoryFactory() {
    }

    /**
     * The result of choosing a repository. {@code notice} explains a fallback,
     * or is {@code null} when the requested storage was used.
     */
    public record Selection(LibraryRepository repository, String notice) {
    }

    /**
     * Builds the repository for {@code requested}.
     *
     * <p>If SQLite is requested but its driver is missing (for example when the app
     * is compiled with plain {@code javac} instead of Maven), this falls back to CSV
     * storage rather than failing, and says so in the notice.
     *
     * @param file the file to use, or {@code null} for the type's default inside {@code dataDirectory}
     */
    public static Selection create(StorageType requested, Path dataDirectory, Path file) {
        return create(requested, dataDirectory, file, SqliteLibraryRepository.isDriverAvailable());
    }

    /** Same as above, with the driver check passed in so tests can cover both cases. */
    static Selection create(StorageType requested, Path dataDirectory, Path file, boolean sqliteAvailable) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(dataDirectory, "dataDirectory");

        if (requested == StorageType.SQLITE && !sqliteAvailable) {
            Path csvFile = file == null ? StorageType.CSV.defaultFile(dataDirectory) : withCsvExtension(file);
            return new Selection(new CsvLibraryRepository(csvFile),
                    "SQLite driver not found on the classpath, so the library will be saved as CSV instead.");
        }

        Path target = file == null ? requested.defaultFile(dataDirectory) : file;
        LibraryRepository repository = switch (requested) {
            case SQLITE -> new SqliteLibraryRepository(target);
            case CSV -> new CsvLibraryRepository(target);
        };
        return new Selection(repository, null);
    }

    private static Path withCsvExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return file.resolveSibling(base + ".csv");
    }
}
