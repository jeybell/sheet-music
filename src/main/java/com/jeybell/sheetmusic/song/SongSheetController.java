package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.song.dto.SongSheetRequest;
import com.jeybell.sheetmusic.song.dto.SongSheetResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SongSheetController {

    private final SongSheetService songSheetService;

    public SongSheetController(SongSheetService songSheetService) {
        this.songSheetService = songSheetService;
    }

    @PostMapping("/api/songs/{songId}/sheets")
    public ResponseEntity<SongSheetResponse> createSheet(
            @PathVariable Long songId,
            @Valid @RequestBody SongSheetRequest request
    ) {
        SongSheetResponse response = songSheetService.createSheet(songId, request);
        return ResponseEntity
                .created(URI.create("/api/song-sheets/" + response.id()))
                .body(response);
    }

    @GetMapping("/api/songs/{songId}/sheets")
    public ResponseEntity<List<SongSheetResponse>> getSheets(@PathVariable Long songId) {
        return ResponseEntity.ok(songSheetService.getSheets(songId));
    }

    @PutMapping("/api/song-sheets/{sheetId}")
    public ResponseEntity<SongSheetResponse> updateSheet(
            @PathVariable Long sheetId,
            @Valid @RequestBody SongSheetRequest request
    ) {
        return ResponseEntity.ok(songSheetService.updateSheet(sheetId, request));
    }

    @DeleteMapping("/api/song-sheets/{sheetId}")
    public ResponseEntity<Void> deleteSheet(@PathVariable Long sheetId) {
        songSheetService.deleteSheet(sheetId);
        return ResponseEntity.noContent().build();
    }
}
