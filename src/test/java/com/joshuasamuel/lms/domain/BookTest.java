package com.joshuasamuel.lms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("when created")
    class Creation {

        @Test
        @DisplayName("is available and keeps its title and author")
        void newBookIsAvailable() {
            Book book = new Book("Dune", "Frank Herbert");

            assertEquals("Dune", book.getTitle());
            assertEquals("Frank Herbert", book.getAuthor());
            assertFalse(book.isBorrowed());
        }

        @Test
        @DisplayName("trims and collapses whitespace in title and author")
        void cleansWhitespace() {
            Book book = new Book("  The   Hobbit ", "\tJ.R.R.  Tolkien  ");

            assertEquals("The Hobbit", book.getTitle());
            assertEquals("J.R.R. Tolkien", book.getAuthor());
        }

        @ParameterizedTest(name = "title [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("rejects a missing title")
        void rejectsBlankTitle(String title) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Book(title, "Author"));
            assertEquals("Title must not be empty.", e.getMessage());
        }

        @ParameterizedTest(name = "author [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("rejects a missing author")
        void rejectsBlankAuthor(String author) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Book("Title", author));
            assertEquals("Author must not be empty.", e.getMessage());
        }

        @Test
        @DisplayName("can be restored in a borrowed state")
        void restoreKeepsLoanState() {
            assertTrue(Book.restore("Dune", "Frank Herbert", true).isBorrowed());
            assertFalse(Book.restore("Dune", "Frank Herbert", false).isBorrowed());
        }
    }

    @Nested
    @DisplayName("borrowBook()")
    class Borrowing {

        @Test
        @DisplayName("marks an available book as borrowed and returns true")
        void borrowsAvailableBook() {
            Book book = new Book("Dune", "Frank Herbert");

            assertTrue(book.borrowBook());
            assertTrue(book.isBorrowed());
        }

        @Test
        @DisplayName("returns false and changes nothing if already borrowed")
        void refusesSecondBorrow() {
            Book book = new Book("Dune", "Frank Herbert");
            book.borrowBook();

            assertFalse(book.borrowBook());
            assertTrue(book.isBorrowed());
        }
    }

    @Nested
    @DisplayName("returnBook()")
    class Returning {

        @Test
        @DisplayName("marks a borrowed book as available and returns true")
        void returnsBorrowedBook() {
            Book book = new Book("Dune", "Frank Herbert");
            book.borrowBook();

            assertTrue(book.returnBook());
            assertFalse(book.isBorrowed());
        }

        @Test
        @DisplayName("returns false if the book was not borrowed")
        void refusesReturnOfAvailableBook() {
            Book book = new Book("Dune", "Frank Herbert");

            assertFalse(book.returnBook());
            assertFalse(book.isBorrowed());
        }

        @Test
        @DisplayName("allows the book to be borrowed again afterwards")
        void canBeBorrowedAgain() {
            Book book = new Book("Dune", "Frank Herbert");
            book.borrowBook();
            book.returnBook();

            assertTrue(book.borrowBook());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("shows Available for a book on the shelf")
        void available() {
            assertEquals("Dune by Frank Herbert (Available)", new Book("Dune", "Frank Herbert").toString());
        }

        @Test
        @DisplayName("shows Borrowed for a book on loan")
        void borrowed() {
            Book book = new Book("Dune", "Frank Herbert");
            book.borrowBook();

            assertEquals("Dune by Frank Herbert (Borrowed)", book.toString());
        }
    }

    @Nested
    @DisplayName("equality and title matching")
    class Equality {

        @Test
        @DisplayName("books with the same title and author are equal, ignoring case and loan state")
        void equalIgnoringCaseAndState() {
            Book a = new Book("Dune", "Frank Herbert");
            Book b = new Book("DUNE", "frank herbert");
            b.borrowBook();

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("books with different authors are not equal")
        void differentAuthorsNotEqual() {
            assertNotEquals(new Book("Dune", "Frank Herbert"), new Book("Dune", "Brian Herbert"));
        }

        @Test
        @DisplayName("hasTitle ignores case and extra spaces")
        void hasTitleIsForgiving() {
            Book book = new Book("The Hobbit", "J.R.R. Tolkien");

            assertTrue(book.hasTitle("the hobbit"));
            assertTrue(book.hasTitle("  THE   HOBBIT "));
            assertFalse(book.hasTitle("Hobbit"));
            assertFalse(book.hasTitle(null));
        }
    }
}
