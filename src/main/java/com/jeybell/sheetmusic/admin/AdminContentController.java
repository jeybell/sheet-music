package com.jeybell.sheetmusic.admin;

import com.jeybell.sheetmusic.admin.dto.AdminDeletedSetlistResponse;
import com.jeybell.sheetmusic.admin.dto.AdminDeletedSongResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 콘텐츠(휴지통) 관리 API. soft delete 된 곡/콘티 조회 및 복구.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping("/songs/deleted")
    public ResponseEntity<List<AdminDeletedSongResponse>> getDeletedSongs() {
        return ResponseEntity.ok(adminContentService.getDeletedSongs());
    }

    @PostMapping("/songs/{id}/restore")
    public ResponseEntity<Void> restoreSong(@PathVariable("id") Long id) {
        adminContentService.restoreSong(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/setlists/deleted")
    public ResponseEntity<List<AdminDeletedSetlistResponse>> getDeletedSetlists() {
        return ResponseEntity.ok(adminContentService.getDeletedSetlists());
    }

    @PostMapping("/setlists/{id}/restore")
    public ResponseEntity<Void> restoreSetlist(@PathVariable("id") Long id) {
        adminContentService.restoreSetlist(id);
        return ResponseEntity.noContent().build();
    }
}
