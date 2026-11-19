package com.cells.backend.participation;

import jakarta.persistence.*;

import java.time.Instant;

/** A member's written understanding. Final once saved: there is no setter and no update. */
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

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    protected DevotionSubmission() {
        // required by JPA
    }

    public DevotionSubmission(Long devotionId, Long roomId, Long memberId, String content) {
        this.devotionId = devotionId;
        this.roomId = roomId;
        this.memberId = memberId;
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getDevotionId() { return devotionId; }
    public Long getRoomId() { return roomId; }
    public Long getMemberId() { return memberId; }
    public String getContent() { return content; }
    public Instant getSubmittedAt() { return submittedAt; }

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