package com.joshuasamuel.lms.domain;

import com.joshuasamuel.lms.exception.BookAlreadyBorrowedException;
import com.joshuasamuel.lms.exception.BookNotBorrowedException;
import com.joshuasamuel.lms.exception.BookNotFoundException;
import com.joshuasamuel.lms.exception.DuplicateBookException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Library")
class LibraryTest {

    private Library library;
    private Book dune;
    private Book emma;
    private Book hobbit;

    @BeforeEach
    void setUp() {
        library = new Library();
        dune = new Book("Dune", "Frank Herbert");
        emma = new Book("Emma", "Jane Austen");
        hobbit = new Book("The Hobbit", "J.R.R. Tolkien");
        library.addBook(dune);
        library.addBook(emma);
        library.addBook(hobbit);
    }

    private static List<String> titles(List<Book> books) {
        return books.stream().map(Book::getTitle).toList();
    }

    @Nested
    @DisplayName("addBook()")
    class AddBook {

        @Test
        @DisplayName("adds a book to the collection")
        void addsBook() {
            library.addBook(new Book("Beloved", "Toni Morrison"));

            assertEquals(4, library.size());
            assertTrue(library.findBook("Beloved").isPresent());
        }

        @Test
        @DisplayName("rejects a title that is already held, ignoring case and spaces")
        void rejectsDuplicateTitle() {
            DuplicateBookException e = assertThrows(DuplicateBookException.class,
                    () -> library.addBook(new Book("  dune ", "Someone Else")));

            assertEquals("A book titled \"Dune\" is already in the library.", e.getMessage());
            assertEquals(3, library.size());
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> library.addBook(null));
        }

        @Test
        @DisplayName("a new library starts empty")
        void newLibraryIsEmpty() {
            assertTrue(new Library().isEmpty());
        }

        @Test
        @DisplayName("the loading constructor also rejects duplicate titles")
        void constructorRejectsDuplicates() {
            assertThrows(DuplicateBookException.class,
                    () -> new Library(List.of(new Book("Dune", "A"), new Book("DUNE", "B"))));
        }
    }

    @Nested
    @DisplayName("viewing books")
    class Viewing {

        @Test
        @DisplayName("viewAvailableBooks leaves out borrowed books and keeps insertion order")
        void availableExcludesBorrowed() {
            library.borrowBook("Emma");

            assertEquals(List.of(dune, hobbit), library.viewAvailableBooks());
        }

        @Test
        @DisplayName("viewAvailableBooks is empty when everything is on loan")
        void availableEmptyWhenAllBorrowed() {
            library.borrowBook("Dune");
            library.borrowBook("Emma");
            library.borrowBook("The Hobbit");

            assertTrue(library.viewAvailableBooks().isEmpty());
        }

        @Test
        @DisplayName("returned lists are read-only snapshots")
        void listsAreReadOnly() {
            List<Book> available = library.viewAvailableBooks();
            List<Book> all = library.viewAllBooks();

            assertThrows(UnsupportedOperationException.class, () -> available.add(new Book("X", "Y")));
            assertThrows(UnsupportedOperationException.class, () -> all.clear());
            assertEquals(3, library.size());
        }

        @Test
        @DisplayName("can sort all books by title")
        void sortByTitle() {
            library.addBook(new Book("anna karenina", "Leo Tolstoy"));

            assertEquals(List.of("anna karenina", "Dune", "Emma", "The Hobbit"),
                    titles(library.viewAllBooks(SortOrder.TITLE)));
        }

        @Test
        @DisplayName("can sort all books by author, then title")
        void sortByAuthor() {
            library.addBook(new Book("Children of Dune", "Frank Herbert"));

            assertEquals(List.of("Children of Dune", "Dune", "The Hobbit", "Emma"),
                    titles(library.viewAllBooks(SortOrder.AUTHOR)));
        }

        @Test
        @DisplayName("can sort available books")
        void sortAvailable() {
            library.borrowBook("Dune");

            assertEquals(List.of("The Hobbit", "Emma"), titles(library.viewAvailableBooks(SortOrder.AUTHOR)));
        }
    }

    @Nested
    @DisplayName("searching")
    class Searching {

        @Test
        @DisplayName("searchBook finds a book by title, ignoring case and spaces")
        void searchBookIgnoresCase() {
            assertSame(hobbit, library.searchBook("  the HOBBIT "));
        }

        @Test
        @DisplayName("searchBook throws when the title does not exist")
        void searchBookMissing() {
            BookNotFoundException e = assertThrows(BookNotFoundException.class, () -> library.searchBook("Ulysses"));

            assertEquals("No book titled \"Ulysses\" was found.", e.getMessage());
            assertEquals("Ulysses", e.getTitle());
        }

        @ParameterizedTest(name = "[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("searchBook treats a blank title as not found")
        void searchBookBlank(String title) {
            assertThrows(BookNotFoundException.class, () -> library.searchBook(title));
        }

        @Test
        @DisplayName("findBook returns empty instead of throwing")
        void findBookEmpty() {
            assertTrue(library.findBook("Ulysses").isEmpty());
        }

        @Test
        @DisplayName("search matches part of a title")
        void searchByPartialTitle() {
            assertEquals(List.of(hobbit), library.search("hobb"));
        }

        @Test
        @DisplayName("search matches part of an author's name")
        void searchByAuthor() {
            assertEquals(List.of(emma), library.search("AUSTEN"));
        }

        @Test
        @DisplayName("search returns matches from both fields, sorted by title")
        void searchBothFieldsSorted() {
            library.addBook(new Book("Mansfield Park", "Jane Austen"));
            library.addBook(new Book("Jane Eyre", "Charlotte Bronte"));

            assertEquals(List.of("Emma", "Jane Eyre", "Mansfield Park"), titles(library.search("jane")));
        }

        @Test
        @DisplayName("search includes borrowed books")
        void searchIncludesBorrowed() {
            library.borrowBook("Dune");

            assertEquals(List.of(dune), library.search("dune"));
        }

        @ParameterizedTest(name = "[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "zzz"})
        @DisplayName("search returns nothing for a blank or unmatched query")
        void searchNoResults(String query) {
            assertTrue(library.search(query).isEmpty());
        }
    }

    @Nested
    @DisplayName("borrowBook()")
    class Borrowing {

        @Test
        @DisplayName("lends out an available book")
        void borrowsBook() {
            assertTrue(library.borrowBook("dune"));
            assertTrue(dune.isBorrowed());
        }

        @Test
        @DisplayName("throws if the book is already borrowed")
        void alreadyBorrowed() {
            library.borrowBook("Dune");

            BookAlreadyBorrowedException e = assertThrows(BookAlreadyBorrowedException.class,
                    () -> library.borrowBook("Dune"));
            assertEquals("\"Dune\" is already borrowed.", e.getMessage());
            assertTrue(dune.isBorrowed());
        }

        @Test
        @DisplayName("throws if the book does not exist")
        void missingBook() {
            assertThrows(BookNotFoundException.class, () -> library.borrowBook("Ulysses"));
        }
    }

    @Nested
    @DisplayName("returnBook()")
    class Returning {

        @Test
        @DisplayName("takes back a borrowed book")
        void returnsBook() {
            library.borrowBook("Emma");

            assertTrue(library.returnBook("EMMA"));
            assertFalse(emma.isBorrowed());
            assertTrue(library.viewAvailableBooks().contains(emma));
        }

        @Test
        @DisplayName("throws if the book was not borrowed")
        void notBorrowed() {
            BookNotBorrowedException e = assertThrows(BookNotBorrowedException.class,
                    () -> library.returnBook("Emma"));
            assertEquals("\"Emma\" cannot be returned because it is not borrowed.", e.getMessage());
        }

        @Test
        @DisplayName("throws if the book does not exist")
        void missingBook() {
            assertThrows(BookNotFoundException.class, () -> library.returnBook("Ulysses"));
        }

        @Test
        @DisplayName("a returned book can be borrowed again")
        void fullLoanCycle() {
            library.borrowBook("Dune");
            library.returnBook("Dune");

            assertTrue(library.borrowBook("Dune"));
        }
    }
}
