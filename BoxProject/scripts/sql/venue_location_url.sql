-- Run once for an existing venues table. Safe to run again.
-- Existing venues may leave the optional map link unset.
ALTER TABLE venues ADD COLUMN IF NOT EXISTS location_url TEXT;
