-- A submission must belong to a devotion of the SAME room (the pair is enforced by a composite foreign key).
ALTER TABLE devotions ADD CONSTRAINT uq_devotions_id_room UNIQUE (id, room_id);

CREATE TABLE devotion_submissions (
                                      id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      devotion_id  BIGINT        NOT NULL,
                                      room_id      BIGINT        NOT NULL,
                                      member_id    BIGINT        NOT NULL REFERENCES members (user_id),
                                      content      VARCHAR(2000) NOT NULL,
                                      submitted_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                      CONSTRAINT fk_submissions_devotion_room FOREIGN KEY (devotion_id, room_id) REFERENCES devotions (id, room_id),
                                      CONSTRAINT chk_submissions_content CHECK (length(btrim(content)) >= 1)
);

-- One submission per member per devotion: this is the safety net behind the engine's own check.
CREATE UNIQUE INDEX uq_submissions_devotion_member ON devotion_submissions (devotion_id, member_id);
CREATE INDEX idx_submissions_room ON devotion_submissions (room_id);
CREATE INDEX idx_submissions_member ON devotion_submissions (member_id);

CREATE TABLE daily_exemptions (
                                  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  room_id        BIGINT      NOT NULL REFERENCES rooms (id),
                                  member_id      BIGINT      NOT NULL REFERENCES members (user_id),
                                  exemption_date DATE        NOT NULL,
                                  granted_by     BIGINT      NOT NULL REFERENCES users (id),
                                  granted_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  revoked_at     TIMESTAMPTZ,
                                  revoked_by     BIGINT      REFERENCES users (id),
                                  CONSTRAINT chk_exemptions_revoked CHECK ((revoked_at IS NULL) = (revoked_by IS NULL))
);

-- One ACTIVE exemption per member, room and day. A revoked one stays as history, and may be granted again.
CREATE UNIQUE INDEX uq_exemptions_active ON daily_exemptions (room_id, member_id, exemption_date) WHERE revoked_at IS NULL;
CREATE INDEX idx_exemptions_room_date ON daily_exemptions (room_id, exemption_date);