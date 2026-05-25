create table songs (
    song_id bigserial primary key,
    title varchar(255) not null,
    artist varchar(255),
    composer varchar(255),
    memo text,
    created_at timestamp not null default now(),
    deleted_at timestamp null
);

create table song_sheets (
    song_sheet_id bigserial primary key,
    song_id bigint not null,
    sheet_key varchar(20),
    version_name varchar(100),
    memo text,
    created_at timestamp not null default now(),
    deleted_at timestamp null,
    constraint fk_song_sheets_song
        foreign key (song_id)
        references songs(song_id)
);

create table song_files (
    song_file_id bigserial primary key,
    song_sheet_id bigint not null,
    original_file_name varchar(255) not null,
    stored_file_name varchar(255) not null,
    file_path varchar(1000) not null,
    content_type varchar(100),
    file_size bigint,
    created_at timestamp not null default now(),
    deleted_at timestamp null,
    constraint fk_song_files_song_sheet
        foreign key (song_sheet_id)
        references song_sheets(song_sheet_id)
);

create table setlists (
    setlist_id bigserial primary key,
    service_date date not null,
    service_type varchar(50),
    title varchar(255),
    memo text,
    created_at timestamp not null default now(),
    deleted_at timestamp null
);

create table setlist_items (
    setlist_item_id bigserial primary key,
    setlist_id bigint not null,
    song_id bigint not null,
    song_sheet_id bigint,
    order_no integer not null,
    memo text,
    constraint fk_setlist_items_setlist
        foreign key (setlist_id)
        references setlists(setlist_id),
    constraint fk_setlist_items_song
        foreign key (song_id)
        references songs(song_id),
    constraint fk_setlist_items_song_sheet
        foreign key (song_sheet_id)
        references song_sheets(song_sheet_id)
);

create index idx_songs_title on songs(title);
create index idx_song_sheets_song_id on song_sheets(song_id);
create index idx_song_sheets_sheet_key on song_sheets(sheet_key);
create index idx_song_files_song_sheet_id on song_files(song_sheet_id);
create index idx_setlists_service_date on setlists(service_date);
create index idx_setlist_items_setlist_id on setlist_items(setlist_id);