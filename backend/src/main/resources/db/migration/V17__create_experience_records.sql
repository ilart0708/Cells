CREATE TABLE experience_records (
                                    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    member_id     BIGINT      NOT NULL REFERENCES members (user_id),
                                    submission_id BIGINT      NOT NULL REFERENCES devotion_submissions (id),
                                    points        INTEGER     NOT NULL,
                                    awarded_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    CONSTRAINT chk_experience_points CHECK (points > 0)
);

-- One award per submission: the safety net behind the engine's own transaction.
CREATE UNIQUE INDEX uq_experience_submission ON experience_records (submission_id);
CREATE INDEX idx_experience_member ON experience_records (member_id);

-- Answers given before this table existed earn the standard 10 EXP, dated when they were given.
INSERT INTO experience_records (member_id, submission_id, points, awarded_at)
SELECT member_id, id, 10, submitted_at FROM devotion_submissions
    ON CONFLICT DO NOTHING;