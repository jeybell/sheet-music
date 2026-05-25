package com.jeybell.sheetmusic.song;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongSheetRepository extends JpaRepository<SongSheet, Long> {

    List<SongSheet> findAllBySongSongIdAndDeletedAtIsNullOrderBySongSheetIdAsc(Long songId);

    Optional<SongSheet> findBySongSheetIdAndDeletedAtIsNull(Long songSheetId);
}
