-- Leaders who register themselves (no invitation from a pastor).
-- A self-registered church carries the pastor's name (and optional email) as typed by the leader.
-- It stays "not verified" until an administrator verifies it. Nothing is linked to a pastor account.
ALTER TABLE churches
    ADD COLUMN self_registered BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verified        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN pastor_name     VARCHAR(150),
    ADD COLUMN pastor_email    VARCHAR(255);

-- Administrator-created churches stay unique by name. Self-registered churches may repeat a name:
-- a self-registered leader is never attached to an existing church by name, so nobody can join
-- another church's member list by typing its name.
DROP INDEX uq_churches_name;
CREATE UNIQUE INDEX uq_churches_name ON churches (lower(name)) WHERE NOT self_registered;
