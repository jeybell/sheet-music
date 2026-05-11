ALTER TABLE songs
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_songs_deleted_at ON songs(deleted_at);
