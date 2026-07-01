package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.song.dto.PopularSongResponse;
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

    /**
     * 키순 정렬 전용. 곡의 대표 키(활성 악보 중 최소 sheetKey)로 오름차순 정렬한다.
     * 키가 없는 곡은 마지막에 온다. 필터 조건은 searchSongs 와 동일.
     * 정렬을 쿼리에 고정하므로 Pageable 의 Sort 는 무시(unsorted)로 전달할 것.
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
            order by (
              select min(ss2.sheetKey) from SongSheet ss2
              where ss2.song = s and ss2.deletedAt is null
            ) asc nulls last
            """)
    Page<Song> searchSongsOrderByKey(@Param("query") String query, @Param("songKey") String songKey,
                                     @Param("tags") List<String> tags, @Param("tagCount") long tagCount,
                                     Pageable pageable);

    @Query("select distinct t from Song s join s.tags t where s.deletedAt is null order by t")
    List<String> findAllTags();

    /**
     * 자주 쓰는 곡: 활성 콘티(setlist)에 담긴 setlist_items 참조 횟수 기준 내림차순.
     */
    @Query("""
            select new com.jeybell.sheetmusic.song.dto.PopularSongResponse(
                s.songId, s.title, s.artist, count(i))
            from SetlistItem i
              join i.song s
              join i.setlist sl
            where s.deletedAt is null and sl.deletedAt is null
            group by s.songId, s.title, s.artist
            order by count(i) desc
            """)
    List<PopularSongResponse> findPopularSongs(Pageable pageable);

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
