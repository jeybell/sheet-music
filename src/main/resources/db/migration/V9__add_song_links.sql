create table song_links (
    link_id  bigserial primary key,
    song_id  bigint       not null references songs(song_id),
    title    varchar(100) not null default '',
    url      varchar(500) not null,
    sort_no  integer      not null default 0
);
create index idx_song_links_song_id on song_links(song_id);

-- 기존 youtube_url 데이터 이전
insert into song_links (song_id, title, url, sort_no)
select song_id, 'YouTube', youtube_url, 0
from songs
where youtube_url is not null and deleted_at is null;
