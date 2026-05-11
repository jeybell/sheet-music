CREATE TABLE IF NOT EXISTS song_sheets (
    id BIGSERIAL PRIMARY KEY,
    song_id BIGINT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    sheet_key VARCHAR(20) NOT NULL,
    memo TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_original BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'songs'
          AND column_name = 'original_key'
    ) THEN
        INSERT INTO song_sheets (song_id, sheet_key, is_default, is_original)
        SELECT s.id, s.original_key, TRUE, TRUE
        FROM songs s
        WHERE s.original_key IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM song_sheets ss
              WHERE ss.song_id = s.id
                AND ss.sheet_key = s.original_key
                AND ss.deleted_at IS NULL
          );
    END IF;
END $$;

ALTER TABLE song_files
    ADD COLUMN IF NOT EXISTS song_sheet_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'song_files'
          AND column_name = 'song_id'
    ) THEN
        UPDATE song_files sf
        SET song_sheet_id = ss.id
        FROM song_sheets ss
        WHERE sf.song_sheet_id IS NULL
          AND sf.song_id = ss.song_id
          AND ss.is_default = TRUE
          AND ss.deleted_at IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM song_files
        WHERE song_sheet_id IS NULL
    ) THEN
        ALTER TABLE song_files
            ALTER COLUMN song_sheet_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        JOIN information_schema.constraint_column_usage ccu
          ON tc.constraint_name = ccu.constraint_name
         AND tc.table_schema = ccu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'song_files'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'song_sheet_id'
          AND ccu.table_name = 'song_sheets'
    ) THEN
        ALTER TABLE song_files
            ADD CONSTRAINT fk_song_files_song_sheet_id
                FOREIGN KEY (song_sheet_id) REFERENCES song_sheets(id) ON DELETE CASCADE;
    END IF;
END $$;

ALTER TABLE song_files
    DROP COLUMN IF EXISTS song_id;

ALTER TABLE setlist_items
    ADD COLUMN IF NOT EXISTS song_sheet_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'setlist_items'
          AND column_name = 'song_id'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'setlist_items'
          AND column_name = 'song_key'
    ) THEN
        UPDATE setlist_items si
        SET song_sheet_id = ss.id
        FROM song_sheets ss
        WHERE si.song_sheet_id IS NULL
          AND si.song_id = ss.song_id
          AND si.song_key = ss.sheet_key
          AND ss.deleted_at IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        JOIN information_schema.constraint_column_usage ccu
          ON tc.constraint_name = ccu.constraint_name
         AND tc.table_schema = ccu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'setlist_items'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'song_sheet_id'
          AND ccu.table_name = 'song_sheets'
    ) THEN
        ALTER TABLE setlist_items
            ADD CONSTRAINT fk_setlist_items_song_sheet_id
                FOREIGN KEY (song_sheet_id) REFERENCES song_sheets(id) ON DELETE RESTRICT;
    END IF;
END $$;

ALTER TABLE songs
    DROP COLUMN IF EXISTS original_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_song_sheets_song_id_sheet_key_active
    ON song_sheets(song_id, sheet_key)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_song_sheets_song_id_default_active
    ON song_sheets(song_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_song_sheets_song_id ON song_sheets(song_id);
CREATE INDEX IF NOT EXISTS idx_song_sheets_sheet_key ON song_sheets(sheet_key);
CREATE INDEX IF NOT EXISTS idx_song_files_song_sheet_id ON song_files(song_sheet_id);
CREATE INDEX IF NOT EXISTS idx_setlist_items_song_sheet_id ON setlist_items(song_sheet_id);
