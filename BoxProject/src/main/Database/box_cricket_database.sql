-- Box Cricket Booking Application
-- PostgreSQL 15+ initial schema migration
-- 19 production tables + 1 optional reviews table
--
-- Run this file against an empty database using a role that may install the
-- pgcrypto, postgis, and btree_gist extensions. PostgreSQL is the source of
-- truth; Redis, WebSockets, push delivery, and object storage remain external.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS btree_gist;

SET TIME ZONE 'UTC';

-- Reusable trigger for tables that have an updated_at column.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- A. User and mobile authentication
-- ---------------------------------------------------------------------------

-- 1. Every customer, venue owner, and optional staff member is a user.
CREATE TABLE IF NOT EXISTS users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name           VARCHAR(100) NOT NULL,
    phone               VARCHAR(20) NOT NULL UNIQUE,
    email               VARCHAR(255) UNIQUE,
    password_hash       TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'active',
    phone_verified_at   TIMESTAMPTZ,
    email_verified_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT users_status_check
        CHECK (status IN ('active', 'blocked', 'deleted')),
    CONSTRAINT users_deleted_state_check
        CHECK (status <> 'deleted' OR deleted_at IS NOT NULL)
);

-- 2. Refresh-token sessions for one or more mobile devices.
CREATE TABLE IF NOT EXISTS user_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash  TEXT NOT NULL,
    device_id           VARCHAR(255) NOT NULL,
    ip_address          INET,
    user_agent          TEXT,
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_sessions_expiry_check CHECK (expires_at > created_at),
    CONSTRAINT user_sessions_revocation_check
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

-- 3. App installations and push-notification tokens.
CREATE TABLE IF NOT EXISTS user_devices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_identifier   VARCHAR(255) NOT NULL,
    platform            VARCHAR(20) NOT NULL,
    push_token          TEXT NOT NULL,
    app_version         VARCHAR(30),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_devices_platform_check
        CHECK (platform IN ('android', 'ios')),
    CONSTRAINT user_devices_platform_token_unique UNIQUE (platform, push_token),
    CONSTRAINT user_devices_installation_unique UNIQUE (user_id, device_identifier)
);

-- ---------------------------------------------------------------------------
-- B. Venue management
-- ---------------------------------------------------------------------------

-- 4. Public venue profile, address, status, and searchable map point.
CREATE TABLE IF NOT EXISTS venues (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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

-- 5. Venue authorization. Owners have full CRUD; staff are optional.
CREATE TABLE IF NOT EXISTS venue_members (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id            UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id),
    member_type         VARCHAR(20) NOT NULL,
    permissions         JSONB,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT venue_members_type_check CHECK (member_type IN ('owner', 'staff')),
    CONSTRAINT venue_members_permissions_check
        CHECK (permissions IS NULL OR jsonb_typeof(permissions) = 'object'),
    CONSTRAINT venue_members_user_unique UNIQUE (venue_id, user_id)
);

-- 6. Object-storage references for venue images and videos.
CREATE TABLE IF NOT EXISTS venue_media (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id            UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    media_type          VARCHAR(20) NOT NULL,
    media_url           TEXT NOT NULL,
    thumbnail_url       TEXT,
    display_order       INTEGER NOT NULL DEFAULT 0,
    is_cover            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT venue_media_type_check CHECK (media_type IN ('image', 'video')),
    CONSTRAINT venue_media_order_check CHECK (display_order >= 0)
);

-- 7. Platform-controlled amenity catalogue.
CREATE TABLE IF NOT EXISTS amenities (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL UNIQUE,
    icon_key            VARCHAR(100),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8. Many-to-many assignment of amenities to venues.
CREATE TABLE IF NOT EXISTS venue_amenities (
    venue_id            UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    amenity_id          UUID NOT NULL REFERENCES amenities(id),
    PRIMARY KEY (venue_id, amenity_id)
);

-- ---------------------------------------------------------------------------
-- C. Playing areas, availability, and pricing
-- ---------------------------------------------------------------------------

-- 9. An independently bookable box/ground within a venue.
CREATE TABLE IF NOT EXISTS playing_areas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id            UUID NOT NULL REFERENCES venues(id),
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    sport_type          VARCHAR(30) NOT NULL DEFAULT 'box_cricket',
    surface_type        VARCHAR(50),
    capacity            INTEGER,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 60,
    base_price_per_slot NUMERIC(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT playing_areas_venue_name_unique UNIQUE (venue_id, name),
    CONSTRAINT playing_areas_status_check
        CHECK (status IN ('active', 'inactive', 'maintenance')),
    CONSTRAINT playing_areas_capacity_check CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT playing_areas_slot_duration_check CHECK (slot_duration_minutes > 0),
    CONSTRAINT playing_areas_base_price_check CHECK (base_price_per_slot >= 0)
);

-- 10. Recurring weekly opening hours in the venue's local timezone.
CREATE TABLE IF NOT EXISTS operating_hours (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playing_area_id     UUID NOT NULL REFERENCES playing_areas(id) ON DELETE CASCADE,
    day_of_week         SMALLINT NOT NULL,
    opens_at            TIME,
    closes_at           TIME,
    closes_next_day     BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed           BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT operating_hours_area_day_unique UNIQUE (playing_area_id, day_of_week),
    CONSTRAINT operating_hours_day_check CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT operating_hours_values_check CHECK (
        (is_closed AND opens_at IS NULL AND closes_at IS NULL AND NOT closes_next_day)
        OR
        (
            NOT is_closed
            AND opens_at IS NOT NULL
            AND closes_at IS NOT NULL
            AND (
                (NOT closes_next_day AND closes_at > opens_at)
                OR (closes_next_day AND closes_at <= opens_at)
            )
        )
    )
);

-- 11. Temporary maintenance, holiday, private-event, or owner blocks.
CREATE TABLE IF NOT EXISTS availability_blocks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playing_area_id     UUID NOT NULL REFERENCES playing_areas(id) ON DELETE CASCADE,
    starts_at           TIMESTAMPTZ NOT NULL,
    ends_at             TIMESTAMPTZ NOT NULL,
    block_type          VARCHAR(30) NOT NULL,
    reason              TEXT,
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_blocks_type_check
        CHECK (block_type IN ('maintenance', 'private_event', 'holiday', 'owner_block')),
    CONSTRAINT availability_blocks_range_check CHECK (starts_at < ends_at)
);

-- 12. Time-sensitive overrides of a playing area's base price.
CREATE TABLE IF NOT EXISTS pricing_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playing_area_id     UUID NOT NULL REFERENCES playing_areas(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    day_of_week         SMALLINT,
    starts_at           TIME NOT NULL,
    ends_at             TIME NOT NULL,
    price_per_slot      NUMERIC(12,2) NOT NULL,
    valid_from          DATE,
    valid_until         DATE,
    priority            INTEGER NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pricing_rules_day_check
        CHECK (day_of_week IS NULL OR day_of_week BETWEEN 0 AND 6),
    CONSTRAINT pricing_rules_time_check CHECK (starts_at <> ends_at),
    CONSTRAINT pricing_rules_price_check CHECK (price_per_slot >= 0),
    CONSTRAINT pricing_rules_date_check
        CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from <= valid_until)
);

-- ---------------------------------------------------------------------------
-- D. Booking management
-- ---------------------------------------------------------------------------

-- 13. Authoritative reservation state and immutable price snapshot.
CREATE TABLE IF NOT EXISTS bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_number      VARCHAR(30) NOT NULL UNIQUE,
    user_id             UUID NOT NULL REFERENCES users(id),
    playing_area_id     UUID NOT NULL REFERENCES playing_areas(id),
    starts_at           TIMESTAMPTZ NOT NULL,
    ends_at             TIMESTAMPTZ NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'pending_payment',
    subtotal            NUMERIC(12,2) NOT NULL,
    discount_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(12,2) NOT NULL,
    currency            CHAR(3) NOT NULL DEFAULT 'INR',
    price_details       JSONB,
    customer_notes      TEXT,
    payment_expires_at  TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT bookings_status_check CHECK (status IN (
        'pending_payment', 'confirmed', 'checked_in', 'cancelled',
        'completed', 'expired', 'no_show', 'refunded'
    )),
    CONSTRAINT bookings_range_check CHECK (starts_at < ends_at),
    CONSTRAINT bookings_amounts_check CHECK (
        subtotal >= 0
        AND discount_amount >= 0
        AND tax_amount >= 0
        AND total_amount >= 0
        AND total_amount = subtotal - discount_amount + tax_amount
    ),
    CONSTRAINT bookings_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT bookings_price_details_check
        CHECK (price_details IS NULL OR jsonb_typeof(price_details) = 'object'),
    CONSTRAINT bookings_cancellation_check CHECK (
        (status = 'cancelled' AND cancelled_at IS NOT NULL)
        OR (status <> 'cancelled')
    ),
    CONSTRAINT bookings_no_overlapping_active
        EXCLUDE USING gist (
            playing_area_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        )
        WHERE (status IN ('pending_payment', 'confirmed', 'checked_in'))
);

-- 14. Append-only business timeline for each booking.
CREATE TABLE IF NOT EXISTS booking_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL REFERENCES bookings(id),
    event_type          VARCHAR(40) NOT NULL,
    actor_user_id       UUID REFERENCES users(id),
    message             TEXT,
    metadata            JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT booking_events_metadata_check
        CHECK (metadata IS NULL OR jsonb_typeof(metadata) = 'object')
);

-- ---------------------------------------------------------------------------
-- E. Payments and refunds
-- ---------------------------------------------------------------------------

-- 15. Every payment attempt for a booking.
CREATE TABLE IF NOT EXISTS payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL REFERENCES bookings(id),
    provider            VARCHAR(30) NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    provider_order_id   VARCHAR(255),
    provider_payment_id VARCHAR(255),
    payment_method      VARCHAR(30),
    amount              NUMERIC(12,2) NOT NULL,
    currency            CHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(30) NOT NULL DEFAULT 'created',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT payments_status_check CHECK (status IN (
        'created', 'authorized', 'captured', 'failed',
        'partially_refunded', 'refunded'
    )),
    CONSTRAINT payments_amount_check CHECK (amount > 0),
    CONSTRAINT payments_currency_check CHECK (currency ~ '^[A-Z]{3}$')
);

-- 16. Original payment-provider webhook events for idempotent processing.
CREATE TABLE IF NOT EXISTS payment_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID REFERENCES payments(id),
    provider_event_id   VARCHAR(255) NOT NULL UNIQUE,
    event_type          VARCHAR(100) NOT NULL,
    payload             JSONB NOT NULL,
    processing_status   VARCHAR(20) NOT NULL DEFAULT 'pending',
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    CONSTRAINT payment_events_status_check
        CHECK (processing_status IN ('pending', 'processed', 'failed')),
    CONSTRAINT payment_events_payload_check CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT payment_events_processed_check CHECK (
        processing_status <> 'processed' OR processed_at IS NOT NULL
    )
);

-- 17. Full or partial refunds against captured payments.
CREATE TABLE IF NOT EXISTS refunds (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL REFERENCES payments(id),
    provider_refund_id  VARCHAR(255) UNIQUE,
    amount              NUMERIC(12,2) NOT NULL,
    reason              TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'pending',
    initiated_by        UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT refunds_status_check CHECK (status IN ('pending', 'processed', 'failed')),
    CONSTRAINT refunds_amount_check CHECK (amount > 0)
);

-- ---------------------------------------------------------------------------
-- F. Real-time delivery and notifications
-- ---------------------------------------------------------------------------

-- 18. Transactional outbox consumed by background workers.
CREATE TABLE IF NOT EXISTS outbox_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type          VARCHAR(100) NOT NULL,
    aggregate_type      VARCHAR(50) NOT NULL,
    aggregate_id        UUID NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'pending',
    attempt_count       INTEGER NOT NULL DEFAULT 0,
    available_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT outbox_events_status_check
        CHECK (status IN ('pending', 'processing', 'published', 'failed')),
    CONSTRAINT outbox_events_attempt_check CHECK (attempt_count >= 0),
    CONSTRAINT outbox_events_payload_check CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT outbox_events_published_check CHECK (
        status <> 'published' OR published_at IS NOT NULL
    )
);

-- 19. Durable in-app notification inbox.
CREATE TABLE IF NOT EXISTS notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type   VARCHAR(50) NOT NULL,
    title               VARCHAR(150) NOT NULL,
    body                TEXT NOT NULL,
    data                JSONB,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notifications_data_check
        CHECK (data IS NULL OR jsonb_typeof(data) = 'object')
);

-- ---------------------------------------------------------------------------
-- G. Optional customer feedback
-- ---------------------------------------------------------------------------

-- 20. One verified review per completed booking.
CREATE TABLE IF NOT EXISTS reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL UNIQUE REFERENCES bookings(id),
    user_id             UUID NOT NULL REFERENCES users(id),
    venue_id            UUID NOT NULL REFERENCES venues(id),
    rating              SMALLINT NOT NULL,
    comment             TEXT,
    owner_response      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT reviews_rating_check CHECK (rating BETWEEN 1 AND 5)
);

-- ---------------------------------------------------------------------------
-- Business-integrity triggers
-- ---------------------------------------------------------------------------

-- Prevent successful refunds from exceeding the original payment amount.
CREATE OR REPLACE FUNCTION enforce_refund_limit()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    payment_amount NUMERIC(12,2);
    refunded_total NUMERIC(12,2);
BEGIN
    SELECT amount
      INTO payment_amount
      FROM payments
     WHERE id = NEW.payment_id
     FOR UPDATE;

    IF payment_amount IS NULL THEN
        RAISE EXCEPTION 'Payment % does not exist', NEW.payment_id;
    END IF;

    SELECT COALESCE(SUM(amount), 0)
      INTO refunded_total
      FROM refunds
     WHERE payment_id = NEW.payment_id
       AND status = 'processed'
       AND id <> NEW.id;

    IF NEW.status = 'processed' THEN
        refunded_total := refunded_total + NEW.amount;
    END IF;

    IF refunded_total > payment_amount THEN
        RAISE EXCEPTION 'Processed refunds (%) exceed payment amount (%)',
            refunded_total, payment_amount;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS refunds_limit_trigger ON refunds;
CREATE TRIGGER refunds_limit_trigger
BEFORE INSERT OR UPDATE OF payment_id, amount, status ON refunds
FOR EACH ROW EXECUTE FUNCTION enforce_refund_limit();

-- A review must match the customer, venue, and a completed booking.
CREATE OR REPLACE FUNCTION validate_completed_booking_review()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    booking_user_id UUID;
    booking_venue_id UUID;
    booking_status VARCHAR(30);
BEGIN
    SELECT b.user_id, pa.venue_id, b.status
      INTO booking_user_id, booking_venue_id, booking_status
      FROM bookings b
      JOIN playing_areas pa ON pa.id = b.playing_area_id
     WHERE b.id = NEW.booking_id;

    IF booking_user_id IS NULL THEN
        RAISE EXCEPTION 'Booking % does not exist', NEW.booking_id;
    END IF;

    IF booking_status <> 'completed' THEN
        RAISE EXCEPTION 'Only completed bookings may be reviewed';
    END IF;

    IF NEW.user_id <> booking_user_id OR NEW.venue_id <> booking_venue_id THEN
        RAISE EXCEPTION 'Review user or venue does not match the booking';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS reviews_booking_validation_trigger ON reviews;
CREATE TRIGGER reviews_booking_validation_trigger
BEFORE INSERT OR UPDATE OF booking_id, user_id, venue_id ON reviews
FOR EACH ROW EXECUTE FUNCTION validate_completed_booking_review();

-- Maintain updated_at consistently from the database.
DROP TRIGGER IF EXISTS users_set_updated_at ON users;
CREATE TRIGGER users_set_updated_at BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS user_devices_set_updated_at ON user_devices;
CREATE TRIGGER user_devices_set_updated_at BEFORE UPDATE ON user_devices
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS venues_set_updated_at ON venues;
CREATE TRIGGER venues_set_updated_at BEFORE UPDATE ON venues
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS venue_members_set_updated_at ON venue_members;
CREATE TRIGGER venue_members_set_updated_at BEFORE UPDATE ON venue_members
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS playing_areas_set_updated_at ON playing_areas;
CREATE TRIGGER playing_areas_set_updated_at BEFORE UPDATE ON playing_areas
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS pricing_rules_set_updated_at ON pricing_rules;
CREATE TRIGGER pricing_rules_set_updated_at BEFORE UPDATE ON pricing_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS bookings_set_updated_at ON bookings;
CREATE TRIGGER bookings_set_updated_at BEFORE UPDATE ON bookings
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS payments_set_updated_at ON payments;
CREATE TRIGGER payments_set_updated_at BEFORE UPDATE ON payments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS refunds_set_updated_at ON refunds;
CREATE TRIGGER refunds_set_updated_at BEFORE UPDATE ON refunds
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS reviews_set_updated_at ON reviews;
CREATE TRIGGER reviews_set_updated_at BEFORE UPDATE ON reviews
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Performance indexes
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS user_sessions_active_user_idx
    ON user_sessions (user_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS user_devices_active_user_idx
    ON user_devices (user_id, last_seen_at DESC)
    WHERE is_active;

CREATE INDEX IF NOT EXISTS venues_location_gist_idx
    ON venues USING gist (location);

CREATE INDEX IF NOT EXISTS venues_active_city_idx
    ON venues (city, locality, name)
    WHERE status = 'active' AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS venue_members_user_active_idx
    ON venue_members (user_id, venue_id)
    WHERE is_active;

CREATE UNIQUE INDEX IF NOT EXISTS venue_media_one_cover_idx
    ON venue_media (venue_id)
    WHERE is_cover;

CREATE INDEX IF NOT EXISTS venue_media_display_idx
    ON venue_media (venue_id, display_order, created_at);

CREATE INDEX IF NOT EXISTS venue_amenities_amenity_idx
    ON venue_amenities (amenity_id, venue_id);

CREATE INDEX IF NOT EXISTS playing_areas_active_venue_idx
    ON playing_areas (venue_id, name)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS availability_blocks_area_range_idx
    ON availability_blocks (playing_area_id, starts_at, ends_at);

CREATE INDEX IF NOT EXISTS pricing_rules_lookup_idx
    ON pricing_rules (
        playing_area_id, day_of_week, priority DESC, valid_from, valid_until
    )
    WHERE is_active;

CREATE INDEX IF NOT EXISTS bookings_user_history_idx
    ON bookings (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS bookings_area_schedule_idx
    ON bookings (playing_area_id, starts_at, ends_at, status);

CREATE INDEX IF NOT EXISTS bookings_payment_expiry_idx
    ON bookings (payment_expires_at)
    WHERE status = 'pending_payment';

CREATE INDEX IF NOT EXISTS booking_events_timeline_idx
    ON booking_events (booking_id, created_at, id);

CREATE INDEX IF NOT EXISTS payments_booking_history_idx
    ON payments (booking_id, created_at DESC, id DESC);

CREATE UNIQUE INDEX IF NOT EXISTS payments_provider_order_unique_idx
    ON payments (provider, provider_order_id)
    WHERE provider_order_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS payments_provider_payment_unique_idx
    ON payments (provider, provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS payments_one_effective_capture_idx
    ON payments (booking_id)
    WHERE status IN ('captured', 'partially_refunded', 'refunded');

CREATE INDEX IF NOT EXISTS payment_events_pending_idx
    ON payment_events (received_at, id)
    WHERE processing_status IN ('pending', 'failed');

CREATE INDEX IF NOT EXISTS refunds_payment_history_idx
    ON refunds (payment_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS outbox_events_worker_idx
    ON outbox_events (available_at, created_at, id)
    WHERE status IN ('pending', 'failed');

CREATE INDEX IF NOT EXISTS outbox_events_aggregate_idx
    ON outbox_events (aggregate_type, aggregate_id, created_at);

CREATE INDEX IF NOT EXISTS notifications_user_inbox_idx
    ON notifications (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS notifications_user_unread_idx
    ON notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE INDEX IF NOT EXISTS reviews_venue_recent_idx
    ON reviews (venue_id, created_at DESC, id DESC);

-- Optional initial amenity catalogue. ON CONFLICT makes this seed repeatable.
INSERT INTO amenities (name, icon_key)
VALUES
    ('Parking', 'parking'),
    ('Washroom', 'washroom'),
    ('Floodlights', 'floodlights'),
    ('Drinking water', 'drinking_water'),
    ('Changing room', 'changing_room'),
    ('Cricket equipment', 'cricket_equipment'),
    ('Cafeteria', 'cafeteria')
ON CONFLICT (name) DO NOTHING;

COMMIT;

-- Availability is calculated at request time:
-- operating_hours - availability_blocks - active bookings = available slots.
-- Before inserting a booking, always repeat the authoritative check inside the
-- PostgreSQL transaction. The exclusion constraint is the final race-condition
-- guard, even when Redis currently reports the slot as available.


