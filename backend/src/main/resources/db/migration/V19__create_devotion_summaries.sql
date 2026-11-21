CREATE TABLE devotion_summaries (
                                    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    devotion_id BIGINT        NOT NULL,
                                    room_id     BIGINT        NOT NULL,
                                    type        VARCHAR(20)   NOT NULL,
                                    content     VARCHAR(4000) NOT NULL,
                                    created_by  BIGINT        NOT NULL REFERENCES users (id),
                                    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- The note must belong to a devotion of the SAME room (the pair is enforced by a composite foreign key).
                                    CONSTRAINT fk_summaries_devotion_room FOREIGN KEY (devotion_id, room_id) REFERENCES devotions (id, room_id),
                                    CONSTRAINT chk_summaries_type    CHECK (type IN ('SUMMARY', 'REFLECTION')),
                                    CONSTRAINT chk_summaries_content CHECK (length(btrim(content)) >= 1)
);

-- One summary and one reflection per devotion.
CREATE UNIQUE INDEX uq_summaries_devotion_type ON devotion_summaries (devotion_id, type);
CREATE INDEX idx_summaries_room ON devotion_summaries (room_id);