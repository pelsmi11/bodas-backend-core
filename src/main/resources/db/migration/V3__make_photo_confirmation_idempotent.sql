-- Ensure guest upload confirmation remains idempotent under retries and concurrency.
DO $$
BEGIN
    IF EXISTS (
        SELECT s3_key
        FROM photos
        GROUP BY s3_key
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce unique photo S3 keys: duplicate rows exist';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_photos_s3_key ON photos(s3_key);
