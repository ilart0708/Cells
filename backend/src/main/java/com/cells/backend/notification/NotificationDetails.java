package com.cells.backend.notification;

import com.cells.backend.bible.BibleBook;

import java.time.Instant;

/** Everything needed to write one message, loaded in a single query (no lazy loading while sending). */
public record NotificationDetails(
        Long id, NotificationType kind, Long userId, String email, String fullName,
        Long roomId, Long devotionId, String roomName, String churchName, String leaderName,
        BibleBook book, Integer chapter, Integer verseStart, Integer verseEnd, String devotionTitle,
        Instant emailSentAt, Instant pushSentAt) {

    public String passage() {
        return book.reference(chapter, verseStart, verseEnd);
    }

    /** "Church Name – Room Name", as everywhere else in CELLS. */
    public String roomLabel() {
        return churchName + " – " + roomName;
    }
}
