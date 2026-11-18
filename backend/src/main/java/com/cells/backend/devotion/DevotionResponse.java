package com.cells.backend.devotion;

import com.cells.backend.bible.BibleBook;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The leader's view. editable and restorable are decided by the server, so screens never guess
 * (in Phase 6, editable also becomes false once a member has submitted).
 */
public record DevotionResponse(
        Long id, Long roomId, LocalDate date,
        BibleBook book, String bookName, Integer chapter, Integer verseStart, Integer verseEnd, String reference,
        String title, LearningLevel learningLevel, String instructions,
        DevotionStatus status, boolean editable, boolean restorable,
        Instant createdAt, Instant updatedAt) {

    public static DevotionResponse from(Devotion d, LocalDate today) {
        boolean notPast = !d.getDevotionDate().isBefore(today);
        return new DevotionResponse(
                d.getId(), d.getRoomId(), d.getDevotionDate(),
                d.getBibleBook(), d.getBibleBook().displayName(), d.getChapter(), d.getVerseStart(), d.getVerseEnd(),
                d.getBibleBook().reference(d.getChapter(), d.getVerseStart(), d.getVerseEnd()),
                d.getTitle(), d.getLearningLevel(), d.getInstructions(),
                d.getStatus(),
                d.getStatus() == DevotionStatus.ACTIVE && notPast,
                d.getStatus() == DevotionStatus.CANCELLED && notPast,
                d.getCreatedAt(), d.getUpdatedAt());
    }
}