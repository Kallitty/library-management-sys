package com.joshuasamuel.lms.cli;

import com.joshuasamuel.lms.domain.Book;
import com.joshuasamuel.lms.exception.StorageException;
import com.joshuasamuel.lms.persistence.LibraryRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A test double that keeps saves in memory and can be told to fail. */
final class InMemoryRepository implements LibraryRepository {

    private final List<String> saved = new ArrayList<>();
    private int saveCount;
    private boolean failOnSave;

    @Override
    public List<Book> load() {
        return List.of();
    }

    @Override
    public void save(Collection<Book> books) {
        if (failOnSave) {
            throw new StorageException("Disk is full.");
        }
        saveCount++;
        saved.clear();
        books.forEach(book -> saved.add(book.toString()));
    }

    @Override
    public String describe() {
        return "in-memory test storage";
    }

    void failOnSave() {
        failOnSave = true;
    }

    int saveCount() {
        return saveCount;
    }

    List<String> saved() {
        return saved;
    }
}
