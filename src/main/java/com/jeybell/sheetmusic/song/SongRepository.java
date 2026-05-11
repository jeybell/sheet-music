package com.jeybell.sheetmusic.song;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {

    List<Song> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<Song> findByIdAndDeletedAtIsNull(Long id);
}
