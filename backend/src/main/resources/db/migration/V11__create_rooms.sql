CREATE TABLE rooms (
                       id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       church_id   BIGINT       NOT NULL REFERENCES churches (id),
                       leader_id   BIGINT       NOT NULL REFERENCES cell_group_leaders (user_id),
                       name        VARCHAR(100) NOT NULL,
                       status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                       created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       archived_at TIMESTAMPTZ,
                       CONSTRAINT chk_rooms_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
                       CONSTRAINT chk_rooms_archived CHECK ((status = 'ARCHIVED') = (archived_at IS NOT NULL))
);

-- Only ACTIVE rooms must have unique names, so an archived room frees its name.
CREATE UNIQUE INDEX uq_rooms_leader_name_active ON rooms (leader_id, lower(name)) WHERE status = 'ACTIVE';
CREATE INDEX idx_rooms_leader ON rooms (leader_id);
CREATE INDEX idx_rooms_church ON rooms (church_id);