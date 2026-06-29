package com.jeybell.sheetmusic.setlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    @Query("""
            select distinct sl
            from Setlist sl
            left join fetch sl.items items
            left join fetch items.song
            left join fetch items.songSheet
            where sl.deletedAt is null
            order by sl.serviceDate desc
            """)
    List<Setlist> findAllActive();

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
            left join fetch items.songSheet ss
            left join fetch ss.files
            where sl.shareToken = :token
              and sl.deletedAt is null
            """)
    Optional<Setlist> findByShareToken(@Param("token") String token);
}
