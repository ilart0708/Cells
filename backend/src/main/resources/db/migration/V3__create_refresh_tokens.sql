CREATE TABLE refresh_tokens (
                                id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                token_hash VARCHAR(64)  NOT NULL,
                                family_id  UUID         NOT NULL,
                                expires_at TIMESTAMPTZ  NOT NULL,
                                created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                revoked_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens (user_id);