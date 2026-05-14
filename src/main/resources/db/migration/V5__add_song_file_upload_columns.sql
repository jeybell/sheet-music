ALTER TABLE song_sheets
    ALTER COLUMN version_name DROP NOT NULL;

ALTER TABLE song_files
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stored_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'song_files'
          AND column_name = 'file_name'
    ) THEN
        UPDATE song_files
        SET original_file_name = COALESCE(original_file_name, file_name),
            stored_file_name = COALESCE(stored_file_name, file_name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM song_files
        WHERE original_file_name IS NULL
           OR stored_file_name IS NULL
    ) THEN
        ALTER TABLE song_files
            ALTER COLUMN original_file_name SET NOT NULL,
            ALTER COLUMN stored_file_name SET NOT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_song_files_deleted_at ON song_files(deleted_at);
