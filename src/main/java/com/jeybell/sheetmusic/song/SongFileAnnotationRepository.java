package com.jeybell.sheetmusic.song;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongFileAnnotationRepository extends JpaRepository<SongFileAnnotation, Long> {

    Optional<SongFileAnnotation> findBySongFile_SongFileId(Long songFileId);
}
