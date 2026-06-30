package com.jeybell.sheetmusic.storage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class StorageMigrationController {

    private final StorageMigrationService migrationService;

    public StorageMigrationController(StorageMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/migrate-storage")
    public ResponseEntity<StorageMigrationService.MigrationResult> migrateStorage() {
        StorageMigrationService.MigrationResult result = migrationService.migrateToSongFolders();
        return ResponseEntity.ok(result);
    }
}
