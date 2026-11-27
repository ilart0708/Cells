-- A member or the leader of the room can react to a shared answer (one reaction each, changeable).
-- Counts are shown; who reacted is not. Rows disappear with the answer or the account.
CREATE TABLE answer_reactions (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    submission_id BIGINT      NOT NULL REFERENCES devotion_submissions (id) ON DELETE CASCADE,
    room_id       BIGINT      NOT NULL REFERENCES rooms (id),
    user_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type          VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reactions_type CHECK (type IN ('AMEN', 'ENCOURAGED', 'HELPFUL', 'PRAYING'))
);

-- One reaction per person per answer.
CREATE UNIQUE INDEX uq_reactions_submission_user ON answer_reactions (submission_id, user_id);
