-- Phase 9: member notifications (email + phone push) and the phones that can receive push.

-- One row per "tell this member about this event". The unique key is the idempotency guard: the midnight job,
-- the startup catch-up and the live hooks can all ask for the same notification and it is stored only once.
-- cycle_date is the devotion's own date, so editing a summary tomorrow never re-notifies anyone.
-- No message text is stored here, only what the message is about; the text is built when it is sent.
CREATE TABLE notifications (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users (id),
    type          VARCHAR(30) NOT NULL,
    room_id       BIGINT      NOT NULL REFERENCES rooms (id),
    devotion_id   BIGINT      NOT NULL REFERENCES devotions (id),
    cycle_date    DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts      INT         NOT NULL DEFAULT 0,
    email_sent_at TIMESTAMPTZ,
    push_sent_at  TIMESTAMPTZ,
    last_error    VARCHAR(300),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_event UNIQUE (user_id, type, room_id, devotion_id, cycle_date),
    CONSTRAINT chk_notifications_type CHECK (type IN ('DEVOTION_OPEN', 'YOUR_TURN', 'SUMMARY_POSTED', 'REFLECTION_POSTED')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
);

-- The dispatcher only ever looks at rows that are not finished.
CREATE INDEX idx_notifications_open ON notifications (status, updated_at) WHERE status <> 'SENT';

-- A phone (an FCM registration token) belongs to whoever logged in on it last.
CREATE TABLE device_tokens (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    token        VARCHAR(512) NOT NULL,
    platform     VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_tokens_token UNIQUE (token),
    CONSTRAINT chk_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
