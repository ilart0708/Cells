package com.cells.backend.devotion;

import com.cells.backend.bible.BibleBook;

import java.time.LocalDate;

/** The member's view: the passage and the leader's words, nothing about status or authorship. */
public record MemberDevotionResponse(
        Long id, LocalDate date,
        BibleBook book, String bookName, Integer chapter, Integer verseStart, Integer verseEnd, String reference,
        String title, LearningLevel learningLevel, String instructions) {

    public static MemberDevotionResponse from(Devotion d) {
        return new MemberDevotionResponse(
                d.getId(), d.getDevotionDate(),
                d.getBibleBook(), d.getBibleBook().displayName(), d.getChapter(), d.getVerseStart(), d.getVerseEnd(),
                d.getBibleBook().reference(d.getChapter(), d.getVerseStart(), d.getVerseEnd()),
                d.getTitle(), d.getLearningLevel(), d.getInstructions());
    }
}