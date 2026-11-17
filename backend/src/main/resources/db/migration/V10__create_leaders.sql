CREATE TABLE cell_group_leaders (
                                    user_id    BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                                    church_id  BIGINT      NOT NULL REFERENCES churches (id),
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_leaders_church ON cell_group_leaders (church_id);

CREATE TABLE pastor_leaders (
                                leader_id   BIGINT      PRIMARY KEY REFERENCES cell_group_leaders (user_id) ON DELETE CASCADE,
                                pastor_id   BIGINT      NOT NULL REFERENCES pastors (user_id),
                                assigned_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pastor_leaders_pastor ON pastor_leaders (pastor_id);