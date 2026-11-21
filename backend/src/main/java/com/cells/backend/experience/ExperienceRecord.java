package com.cells.backend.experience;

import jakarta.persistence.*;

import java.time.Instant;

/** EXP earned by one submission. Final once saved: there is no setter, no update and no delete. */
@Entity
@Table(name = "experience_records")
public class ExperienceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt = Instant.now();

    protected ExperienceRecord() {
        // required by JPA
    }

    public ExperienceRecord(Long memberId, Long submissionId, int points) {
        this.memberId = memberId;
        this.submissionId = submissionId;
        this.points = points;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getSubmissionId() { return submissionId; }
    public Integer getPoints() { return points; }
    public Instant getAwardedAt() { return awardedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperienceRecord other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}