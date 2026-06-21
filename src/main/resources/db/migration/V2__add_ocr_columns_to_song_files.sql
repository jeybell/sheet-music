ALTER TABLE song_files
    ADD COLUMN ocr_title     VARCHAR(500),
    ADD COLUMN ocr_key       VARCHAR(20),
    ADD COLUMN ocr_chords    TEXT,
    ADD COLUMN ocr_raw_text  TEXT,
    ADD COLUMN ocr_done      BOOLEAN NOT NULL DEFAULT FALSE;
