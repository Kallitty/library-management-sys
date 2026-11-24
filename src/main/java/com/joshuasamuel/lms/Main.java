package com.joshuasamuel.lms;

import com.joshuasamuel.lms.cli.LibraryCli;
import com.joshuasamuel.lms.domain.Library;
import com.joshuasamuel.lms.exception.LibraryException;
import com.joshuasamuel.lms.persistence.LibraryRepository;
import com.joshuasamuel.lms.persistence.RepositoryFactory;
import com.joshuasamuel.lms.persistence.StorageType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Entry point. Reads the command-line options, loads the saved library and
 * starts the menu.
 *
 * <pre>
 * Options:
 *   --storage=sqlite|csv   storage format (default: sqlite)
 *   --file=PATH            data file (default: data/library.db or data/library.csv)
 *   --help                 show this help
 * </pre>
 */
public final class Main {

    static final Path DATA_DIRECTORY = Path.of("data");

    private static final String USAGE = """
            Usage: java -jar library-management-system.jar [options]

              --storage=sqlite|csv   storage format (default: sqlite)
              --file=PATH            data file (default: data/library.db or data/library.csv)
              --help                 show this help""";

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.exit(run(args, in, out, System.err));
    }

    /** Runs the app and returns the process exit code. Separate from main so it can be tested. */
    static int run(String[] args, BufferedReader in, PrintStream out, PrintStream err) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            err.println(USAGE);
            return 2;
        }
        if (options.help()) {
            out.println(USAGE);
            return 0;
        }

        RepositoryFactory.Selection selection = RepositoryFactory.create(options.storage(), DATA_DIRECTORY, options.file());
        if (selection.notice() != null) {
            out.println("Note: " + selection.notice());
        }
        LibraryRepository repository = selection.repository();

        Library library;
        try {
            library = new Library(repository.load());
        } catch (LibraryException e) {
            // Stop rather than start empty: starting empty would overwrite the user's data on the next save.
            err.println("Could not load the library from " + repository.describe() + ".");
            err.println(e.getMessage());
            err.println("Fix or move the file, then start the app again.");
            return 1;
        }

        new LibraryCli(library, repository, in, out).run();
        return 0;
    }

    /** Parsed command-line options. */
    record Options(StorageType storage, Path file, boolean help) {

        static Options parse(String[] args) {
            StorageType storage = StorageType.SQLITE;
            Path file = null;
            boolean help = false;
            for (String arg : args) {
                if (arg.equals("--help") || arg.equals("-h")) {
                    help = true;
                } else if (arg.startsWith("--storage=")) {
                    storage = StorageType.parse(arg.substring("--storage=".length()));
                } else if (arg.startsWith("--file=") && arg.length() > "--file=".length()) {
                    file = Path.of(arg.substring("--file=".length()));
                } else {
                    throw new IllegalArgumentException("Unknown option: " + arg);
                }
            }
            return new Options(storage, file, help);
        }
    }
}
