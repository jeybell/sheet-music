package com.jeybell.sheetmusic.song;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongFileRepository extends JpaRepository<SongFile, Long> {

    Optional<SongFile> findBySongFileIdAndDeletedAtIsNull(Long songFileId);
}
