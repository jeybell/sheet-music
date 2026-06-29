package com.jeybell.sheetmusic.song;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongLinkRepository extends JpaRepository<SongLink, Long> {
    Optional<SongLink> findByLinkIdAndSongDeletedAtIsNull(Long linkId);
}
