package com.jeybell.sheetmusic.storage;

import com.jeybell.sheetmusic.song.SongFile;
import com.jeybell.sheetmusic.song.SongFileRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageMigrationService {

    private static final Logger log = LoggerFactory.getLogger(StorageMigrationService.class);

    private final SongFileRepository songFileRepository;
    private final StorageService storageService;

    public StorageMigrationService(SongFileRepository songFileRepository, StorageService storageService) {
        this.songFileRepository = songFileRepository;
        this.storageService = storageService;
    }

    @Transactional
    public MigrationResult migrateToSongFolders() {
        List<SongFile> files = songFileRepository.findAll();
        int success = 0, skipped = 0, failed = 0;

        for (SongFile file : files) {
            String oldKey = file.getFilePath();

            // 이미 songs/ 구조면 스킵
            if (oldKey.startsWith("songs/")) {
                skipped++;
                continue;
            }

            Long songId = file.getSongSheet().getSong().getSongId();
            String newKey = "songs/" + songId + "/" + file.getStoredFileName();

            try {
                storageService.copy(oldKey, newKey);
                file.updateFilePath(newKey);
                storageService.delete(oldKey);
                success++;
                log.info("Migrated: {} → {}", oldKey, newKey);
            } catch (Exception e) {
                failed++;
                log.error("Failed to migrate: {}", oldKey, e);
            }
        }

        return new MigrationResult(success, skipped, failed);
    }

    public record MigrationResult(int success, int skipped, int failed) {}
}
