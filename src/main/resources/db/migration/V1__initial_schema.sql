-- Flyway V1: Initial schema (original baseline)
-- Represents the original schema as designed for the wedding photo platform.
-- Baselined for existing Neon DB (Flyway marks V1 as already applied, does not execute it).
-- For fresh installs (CI, new environments), V1 is executed to create the base schema.

-- PostgreSQL native enum for photo moderation status
DO $$ BEGIN
    CREATE TYPE moderation_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Users table (master identity)
CREATE TABLE IF NOT EXISTS users (
    id          SERIAL PRIMARY KEY,
    cognito_id  VARCHAR(255) UNIQUE,
    name        VARCHAR(100),
    email       VARCHAR(255) UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User devices table (sessions/physical devices)
CREATE TABLE IF NOT EXISTS user_devices (
    guest_uuid  UUID PRIMARY KEY,
    user_id     INTEGER NOT NULL,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_device FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Events table
CREATE TABLE IF NOT EXISTS events (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    token       VARCHAR(50) UNIQUE NOT NULL,
    admin_id    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active   BOOLEAN DEFAULT TRUE
);

-- Photos table (ownership + traceability)
CREATE TABLE IF NOT EXISTS photos (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id           INTEGER NOT NULL,
    user_id            INTEGER NOT NULL,
    device_uuid        UUID,
    s3_key             VARCHAR(500) NOT NULL,
    status             moderation_status DEFAULT 'PENDING',
    moderation_details JSONB,
    uploaded_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_photo_event  FOREIGN KEY (event_id)    REFERENCES events(id)        ON DELETE CASCADE,
    CONSTRAINT fk_photo_user   FOREIGN KEY (user_id)     REFERENCES users(id)         ON DELETE CASCADE,
    CONSTRAINT fk_photo_device FOREIGN KEY (device_uuid) REFERENCES user_devices(guest_uuid) ON DELETE SET NULL
);

-- High-performance indexes
CREATE INDEX IF NOT EXISTS idx_photos_event_approved ON photos(event_id) WHERE (status = 'APPROVED');
CREATE INDEX IF NOT EXISTS idx_events_token_search   ON events(token);
CREATE INDEX IF NOT EXISTS idx_photos_user_id        ON photos(user_id);
CREATE INDEX IF NOT EXISTS idx_photos_device_uuid    ON photos(device_uuid);
