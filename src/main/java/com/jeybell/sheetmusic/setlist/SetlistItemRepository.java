package com.jeybell.sheetmusic.setlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
