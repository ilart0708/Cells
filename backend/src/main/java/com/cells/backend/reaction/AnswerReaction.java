package com.cells.backend.reaction;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "answer_reactions")
public class AnswerReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AnswerReaction() {
        // required by JPA
    }

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public Long getUserId() { return userId; }
    public ReactionType getType() { return type; }
}
