create table song_tags (
    song_id bigint not null references songs(song_id),
    tag     varchar(50) not null
);
create index idx_song_tags_song_id on song_tags(song_id);
create index idx_song_tags_tag on song_tags(tag);
