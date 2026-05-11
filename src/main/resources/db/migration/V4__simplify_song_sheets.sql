DROP INDEX IF EXISTS uk_song_sheets_song_id_sheet_key_active;
DROP INDEX IF EXISTS uk_song_sheets_song_id_default_active;

ALTER TABLE song_sheets
    ADD COLUMN IF NOT EXISTS version_name VARCHAR(100),
    ALTER COLUMN sheet_key DROP NOT NULL,
    DROP COLUMN IF EXISTS is_default,
    DROP COLUMN IF EXISTS is_original;
