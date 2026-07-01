package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.setlist.dto.SetlistListRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    /**
     * 콘티 목록 전용 경량 프로젝션 조회.
     * Song 엔티티 대신 필요한 필드(title, artist)만 선택해 EAGER tags/links 로딩을 회피한다.
     * 콘티 × 아이템 평면 행으로 반환되며 서비스에서 콘티 단위로 그룹핑한다.
     */
    @Query("""
            select new com.jeybell.sheetmusic.setlist.dto.SetlistListRow(
                sl.setlistId, sl.serviceDate, sl.title, sl.memo, sl.shareToken, sl.createdAt,
                i.setlistItemId, i.orderNo, s.songId, s.title, s.artist,
                ss.songSheetId, ss.sheetKey, ss.versionName, i.memo
            )
            from Setlist sl
            left join sl.items i
            left join i.song s
            left join i.songSheet ss
            where sl.deletedAt is null
            order by sl.serviceDate desc, sl.setlistId desc, i.orderNo asc
            """)
    List<SetlistListRow> findAllActiveForList();

    @Query("""
            select sl
            from Setlist sl
            left join fetch sl.items items
            left join fetch items.song
            left join fetch items.songSheet
            where sl.setlistId = :setlistId
              and sl.deletedAt is null
            """)
    Optional<Setlist> findActiveById(@Param("setlistId") Long setlistId);

    @Query("""
            select sl
            from Setlist sl
            left join fetch sl.items items
            left join fetch items.song
            left join fetch items.songSheet
            where sl.shareToken = :token
              and sl.deletedAt is null
            """)
    Optional<Setlist> findByShareToken(@Param("token") String token);
}
