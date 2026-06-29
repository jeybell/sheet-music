package com.jeybell.sheetmusic.song;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<Song, Long> {

    @Query("""
            select distinct s
            from Song s
            left join fetch s.sheets sheets
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
              and (:tag is null or :tag member of s.tags)
            order by s.createdAt desc
            """)
    List<Song> searchSongs(@Param("query") String query, @Param("songKey") String songKey, @Param("tag") String tag);

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
