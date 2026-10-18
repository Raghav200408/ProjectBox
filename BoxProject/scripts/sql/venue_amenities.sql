-- Creates only the venue_amenities assignment table.
-- Run venues.sql and amenities.sql before this script.

CREATE TABLE IF NOT EXISTS venue_amenities (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id            BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    amenity_id          BIGINT NOT NULL REFERENCES amenities(id),
    CONSTRAINT venue_amenities_venue_amenity_unique
        UNIQUE (venue_id, amenity_id)
);

-- Upgrades the earlier composite-key development version of this table.
ALTER TABLE venue_amenities
    ADD COLUMN IF NOT EXISTS id BIGINT GENERATED ALWAYS AS IDENTITY;

ALTER TABLE venue_amenities
    DROP CONSTRAINT IF EXISTS venue_amenities_pkey;

ALTER TABLE venue_amenities
    ADD CONSTRAINT venue_amenities_pkey PRIMARY KEY (id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'venue_amenities_venue_amenity_unique'
          AND conrelid = 'venue_amenities'::regclass
    ) THEN
        ALTER TABLE venue_amenities
            ADD CONSTRAINT venue_amenities_venue_amenity_unique
            UNIQUE (venue_id, amenity_id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS venue_amenities_amenity_venue_idx
    ON venue_amenities (amenity_id, venue_id);
