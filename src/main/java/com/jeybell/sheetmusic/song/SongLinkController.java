package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.song.dto.SongLinkRequest;
import com.jeybell.sheetmusic.song.dto.SongLinkResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SongLinkController {

    private final SongLinkService songLinkService;

    public SongLinkController(SongLinkService songLinkService) {
        this.songLinkService = songLinkService;
    }

    @PostMapping("/api/songs/{songId}/links")
    public ResponseEntity<SongLinkResponse> addLink(
            @PathVariable Long songId,
            @Valid @RequestBody SongLinkRequest request
    ) {
        return ResponseEntity.ok(songLinkService.addLink(songId, request));
    }

    @PutMapping("/api/song-links/{linkId}")
    public ResponseEntity<SongLinkResponse> updateLink(
            @PathVariable Long linkId,
            @Valid @RequestBody SongLinkRequest request
    ) {
        return ResponseEntity.ok(songLinkService.updateLink(linkId, request));
    }

    @DeleteMapping("/api/song-links/{linkId}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long linkId) {
        songLinkService.deleteLink(linkId);
        return ResponseEntity.noContent().build();
    }
}
