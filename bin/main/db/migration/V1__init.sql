CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE songs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    original_key VARCHAR(20),
    memo TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE song_files (
    id BIGSERIAL PRIMARY KEY,
    song_id BIGINT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE song_tags (
    id BIGSERIAL PRIMARY KEY,
    song_id BIGINT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_song_tags_song_id_name UNIQUE (song_id, name)
);

CREATE TABLE setlists (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    service_date DATE NOT NULL,
    memo TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE setlist_items (
    id BIGSERIAL PRIMARY KEY,
    setlist_id BIGINT NOT NULL REFERENCES setlists(id) ON DELETE CASCADE,
    song_id BIGINT NOT NULL REFERENCES songs(id) ON DELETE RESTRICT,
    item_order INTEGER NOT NULL,
    song_key VARCHAR(20),
    memo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_setlist_items_setlist_id_item_order UNIQUE (setlist_id, item_order)
);

CREATE INDEX idx_songs_title ON songs(title);
CREATE INDEX idx_song_files_song_id ON song_files(song_id);
CREATE INDEX idx_song_tags_name ON song_tags(name);
CREATE INDEX idx_setlists_service_date ON setlists(service_date);
CREATE INDEX idx_setlist_items_setlist_id ON setlist_items(setlist_id);
CREATE INDEX idx_setlist_items_song_id ON setlist_items(song_id);
