package com.jeybell.sheetmusic.song;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<Song, Long> {

    /**
     * 곡 검색(페이지네이션). 컬렉션(sheets) fetch join 을 쓰지 않아 DB 레벨 페이징이 가능하다.
     * 정렬은 Pageable 의 Sort 로 전달한다. sheets 는 SongResponse 매핑 시 지연 로딩된다.
     */
    @Query("""
            select s
            from Song s
            where s.deletedAt is null
              and (:query is null
                or lower(s.title) like :query
                or lower(s.artist) like :query
                or lower(s.lyrics) like :query)
              and (:songKey is null
                or exists (
                  select 1 from SongSheet ss
                  where ss.song = s
                    and ss.deletedAt is null
                    and lower(ss.sheetKey) like :songKey
                ))
              and (:tagCount = 0
                or (
                  select count(distinct t) from Song sx join sx.tags t
                  where sx = s and t in :tags
                ) = :tagCount)
            """)
    Page<Song> searchSongs(@Param("query") String query, @Param("songKey") String songKey,
                           @Param("tags") List<String> tags, @Param("tagCount") long tagCount,
                           Pageable pageable);

    @Query("select distinct t from Song s join s.tags t where s.deletedAt is null order by t")
    List<String> findAllTags();

    @Query("""
            select distinct s
            from Song s
            left join fetch s.sheets sheets
            where s.songId = :songId
              and s.deletedAt is null
            """)
    Optional<Song> findActiveBySongIdWithSheets(@Param("songId") Long songId);

    @Query("""
            select count(s) > 0
            from Song s
            where trim(lower(s.title)) = trim(lower(:title))
              and s.deletedAt is null
              and (:excludeId is null or s.songId <> :excludeId)
            """)
    boolean existsByTitleIgnoreCase(@Param("title") String title, @Param("excludeId") Long excludeId);
}
