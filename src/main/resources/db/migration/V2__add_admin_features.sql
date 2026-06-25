-- Flyway V2: Add admin panel features
-- Adds columns for event lifecycle, photo moderation audit, user roles, and device blocking.
-- These columns support the admin endpoints (AdminEventService, AdminPhotoService, AdminUserService).

-- Events: add event date, description, status enum, and updated_at for lifecycle management
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_date  TIMESTAMP WITH TIME ZONE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
ALTER TABLE events ADD COLUMN IF NOT EXISTS status      VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE events ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP WITH TIME ZONE;

-- Users: add role for future RBAC and last_login_at for audit
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role          VARCHAR(20) DEFAULT 'GUEST';

-- User devices: add blocked/blocked_at for admin device management
ALTER TABLE user_devices ADD COLUMN IF NOT EXISTS blocked     BOOLEAN DEFAULT FALSE;
ALTER TABLE user_devices ADD COLUMN IF NOT EXISTS blocked_at  TIMESTAMP WITH TIME ZONE;

-- Photos: add moderation audit trail and soft-delete
ALTER TABLE photos ADD COLUMN IF NOT EXISTS moderated_at   TIMESTAMP WITH TIME ZONE;
ALTER TABLE photos ADD COLUMN IF NOT EXISTS moderated_by   VARCHAR(255);
ALTER TABLE photos ADD COLUMN IF NOT EXISTS deleted_at     TIMESTAMP WITH TIME ZONE;

-- Index for pending moderation queue (global admin queue)
CREATE INDEX IF NOT EXISTS idx_photos_status_pending ON photos(status) WHERE (status = 'PENDING' AND deleted_at IS NULL);

-- Index for soft-deleted photos filtering
CREATE INDEX IF NOT EXISTS idx_photos_deleted_at ON photos(deleted_at) WHERE (deleted_at IS NOT NULL);

-- Index for moderation audit (who moderated what)
CREATE INDEX IF NOT EXISTS idx_photos_moderated_by ON photos(moderated_by) WHERE (moderated_by IS NOT NULL);
