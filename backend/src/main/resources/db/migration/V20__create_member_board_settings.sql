-- A member with no row is shown on boards (the default). A row only exists once they have chosen.
CREATE TABLE member_board_settings (
                                       member_id     BIGINT      PRIMARY KEY REFERENCES members (user_id) ON DELETE CASCADE,
                                       show_on_board BOOLEAN     NOT NULL DEFAULT TRUE,
                                       updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);