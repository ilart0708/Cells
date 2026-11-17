CREATE TABLE pastors (
                         user_id    BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                         church_id  BIGINT      NOT NULL REFERENCES churches (id),
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pastors_church ON pastors (church_id);

-- Pastors who accepted an invitation before this table existed (for example the one you
-- created in Postman during Step 2) get linked to the church they were invited to.
INSERT INTO pastors (user_id, church_id)
SELECT u.id, i.church_id
FROM invitations i
         JOIN users u ON u.email = i.email
WHERE i.role = 'PASTOR' AND i.accepted_at IS NOT NULL AND u.role = 'PASTOR'
    ON CONFLICT DO NOTHING;