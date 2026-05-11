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
            order by s.createdAt desc
            """)
    List<Song> findAllActiveWithSheets();

    @Query("""
            select distinct s
            from Song s
            join s.sheets matchedSheet
            left join fetch s.sheets sheets
            where s.deletedAt is null
              and matchedSheet.deletedAt is null
              and matchedSheet.sheetKey = :songKey
            order by s.createdAt desc
            """)
    List<Song> findAllActiveBySongKeyWithSheets(@Param("songKey") String songKey);

    @Query("""
            select distinct s
            from Song s
            left join fetch s.sheets sheets
            where s.id = :id
              and s.deletedAt is null
            """)
    Optional<Song> findActiveByIdWithSheets(@Param("id") Long id);
}
