package com.cells.backend.devotion;

import com.cells.backend.bible.BibleBook;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The leader's view. editable and restorable are decided by the server, so screens never guess.
 * editable is false once the day has passed, the devotion is cancelled, or any member has answered it.
 * submissionCount counts every answer, including those of members who have since left the room.
 */
public record DevotionResponse(
        Long id, Long roomId, LocalDate date,
        BibleBook book, String bookName, Integer chapter, Integer verseStart, Integer verseEnd, String reference,
        String title, LearningLevel learningLevel, String instructions,
        DevotionStatus status, boolean editable, boolean restorable, int submissionCount,
        Instant createdAt, Instant updatedAt) {

    public static DevotionResponse from(Devotion d, LocalDate today, int submissionCount) {
        boolean notPast = !d.getDevotionDate().isBefore(today);
        return new DevotionResponse(
                d.getId(), d.getRoomId(), d.getDevotionDate(),
                d.getBibleBook(), d.getBibleBook().displayName(), d.getChapter(), d.getVerseStart(), d.getVerseEnd(),
                d.getBibleBook().reference(d.getChapter(), d.getVerseStart(), d.getVerseEnd()),
                d.getTitle(), d.getLearningLevel(), d.getInstructions(),
                d.getStatus(),
                d.getStatus() == DevotionStatus.ACTIVE && notPast && submissionCount == 0,
                d.getStatus() == DevotionStatus.CANCELLED && notPast,
                submissionCount,
                d.getCreatedAt(), d.getUpdatedAt());
    }
}