package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.song.dto.SongSheetRequest;
import com.jeybell.sheetmusic.song.dto.SongSheetResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
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
            @PathVariable("songId") Long songId,
            @Valid @RequestBody SongSheetRequest request
    ) {
        SongSheetResponse response = songSheetService.createSheet(songId, request);
        return ResponseEntity
                .created(Objects.requireNonNull(URI.create("/api/song-sheets/" + response.id())))
                .body(response);
    }

    @GetMapping("/api/songs/{songId}/sheets")
    public ResponseEntity<List<SongSheetResponse>> getSheets(@PathVariable("songId") Long songId) {
        return ResponseEntity.ok(songSheetService.getSheets(songId));
    }

    @GetMapping("/api/song-sheets/{songSheetId}")
    public ResponseEntity<SongSheetResponse> getSheet(@PathVariable("songSheetId") Long songSheetId) {
        return ResponseEntity.ok(songSheetService.getSheet(songSheetId));
    }

    @PutMapping("/api/song-sheets/{songSheetId}")
    public ResponseEntity<SongSheetResponse> updateSheet(
            @PathVariable("songSheetId") Long songSheetId,
            @Valid @RequestBody SongSheetRequest request
    ) {
        return ResponseEntity.ok(songSheetService.updateSheet(songSheetId, request));
    }

    @DeleteMapping("/api/song-sheets/{songSheetId}")
    public ResponseEntity<Void> deleteSheet(@PathVariable("songSheetId") Long songSheetId) {
        songSheetService.deleteSheet(songSheetId);
        return ResponseEntity.noContent().build();
    }
}
