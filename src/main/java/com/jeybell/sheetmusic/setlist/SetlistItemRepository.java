package com.jeybell.sheetmusic.setlist;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetlistItemRepository extends JpaRepository<SetlistItem, Long> {

    Optional<SetlistItem> findBySetlistItemIdAndSetlistDeletedAtIsNull(Long setlistItemId);
}
