package com.cells.backend.participation;

import jakarta.persistence.*;

import java.time.Instant;

/** A member's written understanding. Final once saved; the only change is a leader removing the text after a report. */
@Entity
@Table(name = "devotion_submissions")
public class DevotionSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devotion_id", nullable = false)
    private Long devotionId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 2000)
    private String content;

    /** Optional one-line key point, written with the answer. */
    @Column(length = 200)
    private String takeaway;

    /** Marked passages of the content, "start-end,start-end". */
    @Column(length = 100)
    private String highlights;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    /** What replaces the text of an answer the leader removed after a report. */
    public static final String REMOVED_TEXT = "This answer was removed by the leader.";

    protected DevotionSubmission() {
        // required by JPA
    }

    public DevotionSubmission(Long devotionId, Long roomId, Long memberId, String content) {
        this.devotionId = devotionId;
        this.roomId = roomId;
        this.memberId = memberId;
        this.content = content;
    }

    public DevotionSubmission(Long devotionId, Long roomId, Long memberId, String content,
                              String takeaway, String highlights) {
        this(devotionId, roomId, memberId, content);
        this.takeaway = takeaway;
        this.highlights = highlights;
    }

    public Long getId() { return id; }
    public Long getDevotionId() { return devotionId; }
    public Long getRoomId() { return roomId; }
    public Long getMemberId() { return memberId; }
    public String getContent() { return content; }
    public String getTakeaway() { return takeaway; }
    public String getHighlights() { return highlights; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getModeratedAt() { return moderatedAt; }

    /** Removes the text only. The row stays, so participation and EXP are unchanged. */
    public void moderate(Instant now) {
        this.content = REMOVED_TEXT;
        this.takeaway = null;
        this.highlights = null;
        this.moderatedAt = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DevotionSubmission other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}