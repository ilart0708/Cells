CREATE TABLE invitations (
                             id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             token_hash  VARCHAR(64)  NOT NULL,
                             email       VARCHAR(255) NOT NULL,
                             full_name   VARCHAR(150) NOT NULL,
                             role        VARCHAR(20)  NOT NULL,
                             church_id   BIGINT       NOT NULL REFERENCES churches (id),
                             invited_by  BIGINT       NOT NULL REFERENCES users (id),
                             expires_at  TIMESTAMPTZ  NOT NULL,
                             accepted_at TIMESTAMPTZ,
                             revoked_at  TIMESTAMPTZ,
                             created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             CONSTRAINT chk_invitations_role CHECK (role IN ('PASTOR', 'LEADER', 'MEMBER'))
);

CREATE UNIQUE INDEX uq_invitations_token_hash ON invitations (token_hash);
CREATE INDEX idx_invitations_email ON invitations (email);
CREATE INDEX idx_invitations_church ON invitations (church_id);