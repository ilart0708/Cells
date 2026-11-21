package com.cells.backend.history;

import com.cells.backend.bible.BibleBook;

import java.time.LocalDate;

public record DevotionDay(
        Long id, LocalDate date, BibleBook book, Integer chapter, Integer verseStart, Integer verseEnd, String title) {

    public String reference() {
        return book.reference(chapter, verseStart, verseEnd);
    }
}