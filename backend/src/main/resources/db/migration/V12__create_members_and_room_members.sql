CREATE TABLE members (
                         user_id    BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                         church_id  BIGINT      NOT NULL REFERENCES churches (id),
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_members_church ON members (church_id);

CREATE TABLE room_members (
                              id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              room_id              BIGINT      NOT NULL REFERENCES rooms (id),
                              member_id            BIGINT      NOT NULL REFERENCES members (user_id),
                              participation_number INTEGER     NOT NULL,
                              joined_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                              removed_at           TIMESTAMPTZ,
                              CONSTRAINT chk_room_members_number CHECK (participation_number >= 1)
);

-- Only ACTIVE memberships (removed_at IS NULL) are constrained, so a removed member keeps
-- their history row without blocking the number or the room.
CREATE UNIQUE INDEX uq_room_members_number_active
    ON room_members (room_id, participation_number) WHERE removed_at IS NULL;
CREATE UNIQUE INDEX uq_room_members_member_active
    ON room_members (room_id, member_id) WHERE removed_at IS NULL;
CREATE INDEX idx_room_members_member ON room_members (member_id);