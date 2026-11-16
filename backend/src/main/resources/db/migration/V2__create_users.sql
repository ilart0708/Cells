CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name     VARCHAR(150) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_role   CHECK (role IN ('ADMIN', 'PASTOR', 'LEADER', 'MEMBER')),
                       CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'PENDING', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));