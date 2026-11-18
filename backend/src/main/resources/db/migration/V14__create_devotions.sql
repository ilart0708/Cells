CREATE TABLE devotions (
                           id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           room_id        BIGINT        NOT NULL REFERENCES rooms (id),
                           devotion_date  DATE          NOT NULL,
                           bible_book     VARCHAR(30)   NOT NULL,
                           chapter        INTEGER       NOT NULL,
                           verse_start    INTEGER,
                           verse_end      INTEGER,
                           title          VARCHAR(150),
                           learning_level VARCHAR(20)   NOT NULL,
                           instructions   VARCHAR(1000),
                           status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                           created_by     BIGINT        NOT NULL REFERENCES users (id),
                           created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
                           updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
                           cancelled_at   TIMESTAMPTZ,
                           CONSTRAINT chk_devotions_status    CHECK (status IN ('ACTIVE', 'CANCELLED')),
                           CONSTRAINT chk_devotions_cancelled CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)),
                           CONSTRAINT chk_devotions_level     CHECK (learning_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
                           CONSTRAINT chk_devotions_chapter   CHECK (chapter >= 1),
    -- whole chapter | single verse | verse range
                           CONSTRAINT chk_devotions_verses    CHECK (
                               (verse_start IS NULL AND verse_end IS NULL)
                                   OR (verse_start >= 1 AND verse_end IS NULL)
                                   OR (verse_start >= 1 AND verse_end > verse_start))
);

-- Only ACTIVE devotions must be unique per day, so a cancelled one frees its day.
CREATE UNIQUE INDEX uq_devotions_room_date_active ON devotions (room_id, devotion_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_devotions_room_date ON devotions (room_id, devotion_date);