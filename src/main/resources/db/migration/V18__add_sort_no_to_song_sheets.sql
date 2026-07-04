-- 악보 버전 순서(드래그 정렬)용 컬럼. 곡별로 기존 song_sheet_id 순서를 그대로 0-기반 순번으로 백필.
alter table song_sheets add column sort_no integer not null default 0;

update song_sheets ss
set sort_no = sub.rn
from (
    select song_sheet_id,
           row_number() over (partition by song_id order by song_sheet_id) - 1 as rn
    from song_sheets
) sub
where ss.song_sheet_id = sub.song_sheet_id;
