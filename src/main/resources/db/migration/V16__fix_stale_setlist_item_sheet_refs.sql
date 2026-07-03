-- 스테일 콘티 아이템 악보 참조 정리 (fix/setlist-item-update-500)
--
-- 곡 병합/중복 정리 및 제목 대량 업데이트(2026-07-02) 이후, 일부 setlist_items 의
-- song_sheet_id 가 아이템의 song 과 다른 곡 소속이거나 soft-delete 된 시트를 가리키는
-- 스테일 상태가 되었다. 이 상태에서는 PUT /api/setlist-items/{id}(updateItem) 의
-- 악보 소유권 검증에 걸려 memo/연주키/youtube 인라인 수정까지 실패했다.
--
-- 아이템의 song(제목 표시의 기준)은 그대로 두고, 아이템의 song 에 속한 "활성" 악보를
-- 가리키지 않는 song_sheet_id 만 NULL 로 정리한다. 사용자는 필요 시 올바른 악보를
-- 다시 선택할 수 있다. NULL-set 이므로 재실행해도 안전(idempotent)하다.
update setlist_items si
set song_sheet_id = null
where si.song_sheet_id is not null
  and not exists (
      select 1
      from song_sheets ss
      where ss.song_sheet_id = si.song_sheet_id
        and ss.song_id = si.song_id
        and ss.deleted_at is null
  );
