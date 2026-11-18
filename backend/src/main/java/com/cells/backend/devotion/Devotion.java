package com.cells.backend.devotion;

import com.cells.backend.bible.BibleBook;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "devotions")
public class Devotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "devotion_date", nullable = false)
    private LocalDate devotionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "bible_book", nullable = false)
    private BibleBook bibleBook;

    @Column(nullable = false)
    private Integer chapter;

    @Column(name = "verse_start")
    private Integer verseStart;

    @Column(name = "verse_end")
    private Integer verseEnd;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_level", nullable = false)
    private LearningLevel learningLevel;

    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevotionStatus status = DevotionStatus.ACTIVE;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Devotion() {
        // required by JPA
    }

    public Devotion(Long roomId, LocalDate devotionDate, BibleBook bibleBook, int chapter,
                    Integer verseStart, Integer verseEnd, String title,
                    LearningLevel learningLevel, String instructions, Long createdBy) {
        this.roomId = roomId;
        this.devotionDate = devotionDate;
        this.bibleBook = bibleBook;
        this.chapter = chapter;
        this.verseStart = verseStart;
        this.verseEnd = verseEnd;
        this.title = title;
        this.learningLevel = learningLevel;
        this.instructions = instructions;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public LocalDate getDevotionDate() { return devotionDate; }
    public BibleBook getBibleBook() { return bibleBook; }
    public Integer getChapter() { return chapter; }
    public Integer getVerseStart() { return verseStart; }
    public Integer getVerseEnd() { return verseEnd; }
    public String getTitle() { return title; }
    public LearningLevel getLearningLevel() { return learningLevel; }
    public String getInstructions() { return instructions; }
    public DevotionStatus getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(LocalDate devotionDate, BibleBook bibleBook, int chapter,
                       Integer verseStart, Integer verseEnd, String title,
                       LearningLevel learningLevel, String instructions, Instant now) {
        this.devotionDate = devotionDate;
        this.bibleBook = bibleBook;
        this.chapter = chapter;
        this.verseStart = verseStart;
        this.verseEnd = verseEnd;
        this.title = title;
        this.learningLevel = learningLevel;
        this.instructions = instructions;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        this.status = DevotionStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }

    public void restore(Instant now) {
        this.status = DevotionStatus.ACTIVE;
        this.cancelledAt = null;
        this.updatedAt = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Devotion other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}