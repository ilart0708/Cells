-- Reporting and removing abusive answers (Google Play user-generated-content policy).
-- A removed answer keeps its row (so participation and EXP stay correct) but its text is replaced.
ALTER TABLE devotion_submissions ADD COLUMN moderated_at TIMESTAMPTZ;

CREATE TABLE content_reports (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    submission_id BIGINT       NOT NULL REFERENCES devotion_submissions (id),
    room_id       BIGINT       NOT NULL REFERENCES rooms (id),
    reporter_id   BIGINT       NOT NULL REFERENCES users (id),
    reason        VARCHAR(20)  NOT NULL,
    note          VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at   TIMESTAMPTZ,
    resolved_by   BIGINT       REFERENCES users (id),
    CONSTRAINT chk_reports_reason CHECK (reason IN ('OFFENSIVE', 'HARASSMENT', 'INAPPROPRIATE', 'SPAM', 'OTHER')),
    CONSTRAINT chk_reports_status CHECK (status IN ('OPEN', 'DISMISSED', 'REMOVED')),
    CONSTRAINT chk_reports_resolved CHECK ((status = 'OPEN') = (resolved_at IS NULL))
);

-- One report per person per answer.
CREATE UNIQUE INDEX uq_reports_submission_reporter ON content_reports (submission_id, reporter_id);
CREATE INDEX idx_reports_room_status ON content_reports (room_id, status);
