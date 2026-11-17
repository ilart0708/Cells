ALTER TABLE invitations ADD COLUMN room_id BIGINT REFERENCES rooms (id);

CREATE INDEX idx_invitations_room ON invitations (room_id);

-- A member invitation always names its room. Pastor and leader invitations never do.
ALTER TABLE invitations ADD CONSTRAINT chk_invitations_room
    CHECK ((role = 'MEMBER') = (room_id IS NOT NULL));