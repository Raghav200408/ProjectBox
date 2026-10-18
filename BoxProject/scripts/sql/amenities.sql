-- Creates only the platform-controlled amenities catalogue table.

CREATE TABLE IF NOT EXISTS amenities (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE
);

-- Removes the obsolete field from an older development version of this table.
ALTER TABLE amenities DROP COLUMN IF EXISTS icon_key;

CREATE INDEX IF NOT EXISTS amenities_active_name_idx
    ON amenities (name, id)
    WHERE is_active;
