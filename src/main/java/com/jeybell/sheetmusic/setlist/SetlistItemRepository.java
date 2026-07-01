package com.jeybell.sheetmusic.setlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetlistItemRepository extends JpaRepository<SetlistItem, Long> {

    Optional<SetlistItem> findBySetlistItemIdAndSetlistDeletedAtIsNull(Long setlistItemId);

    @Query("""
            select i from SetlistItem i
            where i.setlistItemId in :ids
              and i.setlist.deletedAt is null
            """)
    List<SetlistItem> findAllByIdsAndSetlistActive(@Param("ids") List<Long> ids);

    /** 곡 병합 시: 원본 곡을 참조하던 콘티 아이템을 대상 곡으로 재지정. */
    @Modifying(flushAutomatically = true)
    @Query(value = "update setlist_items set song_id = :targetId where song_id = :sourceId", nativeQuery = true)
    int redirectSongReferences(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

    /** 악보 중복 제거 시: 제거되는 악보를 참조하던 콘티 아이템을 유지 악보로 재지정. */
    @Modifying(flushAutomatically = true)
    @Query(value = "update setlist_items set song_sheet_id = :keptSheetId where song_sheet_id = :removedSheetId",
            nativeQuery = true)
    int redirectSheetReferences(@Param("removedSheetId") Long removedSheetId, @Param("keptSheetId") Long keptSheetId);
}
