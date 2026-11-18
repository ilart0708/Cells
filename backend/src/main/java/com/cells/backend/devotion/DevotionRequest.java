package com.cells.backend.devotion;

import com.cells.backend.bible.BibleBook;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * No verse means the whole chapter. verseStart alone is a single verse. Both is a range.
 * The room is in the URL, so it is not part of the body.
 */
public record DevotionRequest(
        @NotNull(message = "Choose a day.") LocalDate date,
        @NotNull(message = "Choose a book.") BibleBook book,
        @NotNull(message = "Enter the chapter.")
        @Min(value = 1, message = "Chapters start at 1.")
        @Max(value = 150, message = "No book has more than 150 chapters.") Integer chapter,
        @Min(value = 1, message = "Verses start at 1.")
        @Max(value = 176, message = "No chapter has more than 176 verses.") Integer verseStart,
        @Min(value = 1, message = "Verses start at 1.")
        @Max(value = 176, message = "No chapter has more than 176 verses.") Integer verseEnd,
        @Size(max = 150, message = "The title can have at most 150 characters.") String title,
        @NotNull(message = "Choose a learning level.") LearningLevel learningLevel,
        @Size(max = 1000, message = "The instructions can have at most 1000 characters.") String instructions) {
}