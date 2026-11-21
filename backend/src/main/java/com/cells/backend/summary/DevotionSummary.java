package com.cells.backend.summary;

import jakarta.persistence.*;

import java.time.Instant;

/** The leader's summary or reflection for one devotion. Never deleted: editing replaces the text. */
@Entity
@Table(name = "devotion_summaries")
public class DevotionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devotion_id", nullable = false)
    private Long devotionId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryType type;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DevotionSummary() {
        // required by JPA
    }

    public DevotionSummary(Long devotionId, Long roomId, SummaryType type, String content, Long createdBy) {
        this.devotionId = devotionId;
        this.roomId = roomId;
        this.type = type;
        this.content = content;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getDevotionId() { return devotionId; }
    public Long getRoomId() { return roomId; }
    public SummaryType getType() { return type; }
    public String getContent() { return content; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void edit(String newContent, Instant now) {
        this.content = newContent;
        this.updatedAt = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DevotionSummary other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}