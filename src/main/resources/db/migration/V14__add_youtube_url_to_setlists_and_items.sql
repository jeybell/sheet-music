-- 콘티 상세에 YouTube 링크 추가 (콘티 전체 1개 + 곡별 1개)
alter table setlists add column youtube_url varchar(500);
alter table setlist_items add column youtube_url varchar(500);
