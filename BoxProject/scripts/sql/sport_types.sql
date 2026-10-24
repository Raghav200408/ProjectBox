-- Creates the sport catalogue and its many-to-many relationship with venues.
-- Run venues.sql first. The venues table may be empty.
-- This script does not create venues or assign sports to any venue.

BEGIN;

CREATE TABLE IF NOT EXISTS sport_types (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT sport_types_name_not_blank
        CHECK (name = BTRIM(name) AND name <> '')
);

CREATE UNIQUE INDEX IF NOT EXISTS sport_types_name_unique_idx
    ON sport_types (LOWER(name));

CREATE TABLE IF NOT EXISTS venue_sports (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id        BIGINT NOT NULL,
    sport_type_id   BIGINT NOT NULL,
    CONSTRAINT venue_sports_venue_fk
        FOREIGN KEY (venue_id)
        REFERENCES venues(id)
        ON DELETE CASCADE,
    CONSTRAINT venue_sports_sport_type_fk
        FOREIGN KEY (sport_type_id)
        REFERENCES sport_types(id)
        ON DELETE RESTRICT,
    CONSTRAINT venue_sports_unique
        UNIQUE (venue_id, sport_type_id)
);

CREATE INDEX IF NOT EXISTS venue_sports_sport_venue_idx
    ON venue_sports (sport_type_id, venue_id);

INSERT INTO sport_types (name)
VALUES
    ('Box Cricket'),
    ('Football'),
    ('Badminton'),
    ('Basketball'),
    ('Volleyball'),
    ('Tennis')
ON CONFLICT DO NOTHING;

COMMIT;
