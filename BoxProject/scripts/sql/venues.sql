-- Creates only the venues table and its indexes.
-- Run this script before venue_members.sql.
-- PostGIS must be installed on the PostgreSQL server.
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS venues (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    phone               VARCHAR(20) NOT NULL,
    email               VARCHAR(255),
    address_line_1      VARCHAR(200) NOT NULL,
    address_line_2      VARCHAR(200),
    locality            VARCHAR(100) NOT NULL,
    city                VARCHAR(100) NOT NULL,
    state               VARCHAR(100) NOT NULL,
    postal_code         VARCHAR(10) NOT NULL,
    location            GEOGRAPHY(POINT, 4326) NOT NULL,
    timezone            VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    status              VARCHAR(30) NOT NULL DEFAULT 'draft',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT venues_status_check
        CHECK (status IN ('draft', 'pending_approval', 'active', 'suspended', 'closed'))
);

CREATE INDEX IF NOT EXISTS venues_location_gist_idx
    ON venues USING GIST (location);

CREATE INDEX IF NOT EXISTS venues_active_city_idx
    ON venues (city, locality, name)
    WHERE status = 'active' AND deleted_at IS NULL;
