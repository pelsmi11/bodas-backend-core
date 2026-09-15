-- Flyway V4: Add persistent guest-to-event membership.
-- Cognito groups define authorization; this table only defines event membership.

CREATE TABLE event_participants (
    event_id        INTEGER                  NOT NULL,
    user_id         INTEGER                  NOT NULL,
    joined_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT event_participants_pkey PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_event_participant_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_participant_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- The primary key supports event-first lookups. This index supports finding all
-- events for a user immediately after sign-in.
CREATE INDEX idx_event_participants_user_id
    ON event_participants(user_id);

-- Preserve membership already demonstrated by existing photo uploads.
INSERT INTO event_participants (event_id, user_id, joined_at, last_accessed_at)
SELECT
    event_id,
    user_id,
    COALESCE(MIN(uploaded_at), CURRENT_TIMESTAMP),
    COALESCE(MAX(uploaded_at), CURRENT_TIMESTAMP)
FROM photos
GROUP BY event_id, user_id;
