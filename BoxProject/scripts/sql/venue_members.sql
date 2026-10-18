-- Creates only the venue_members table and its indexes.
-- Run roles.sql, users.sql, and venues.sql before this script.

CREATE TABLE IF NOT EXISTS venue_members (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id            BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    member_type         VARCHAR(20) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT venue_members_type_check
        CHECK (member_type IN ('owner', 'staff')),
    CONSTRAINT venue_members_user_unique UNIQUE (venue_id, user_id)
);

-- Removes the obsolete field from an older development version of this table.
ALTER TABLE venue_members DROP COLUMN IF EXISTS permissions;

CREATE INDEX IF NOT EXISTS venue_members_user_active_idx
    ON venue_members (user_id, venue_id)
    WHERE is_active;
