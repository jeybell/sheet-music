create table song_file_annotations (
    annotation_id bigserial primary key,
    song_file_id bigint not null unique references song_files(song_file_id),
    strokes text not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);
