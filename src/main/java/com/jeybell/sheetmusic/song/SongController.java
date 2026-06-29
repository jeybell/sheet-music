package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.song.dto.LyricsRequest;
import com.jeybell.sheetmusic.song.dto.SongRequest;
import com.jeybell.sheetmusic.song.dto.SongResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping
    public ResponseEntity<List<SongResponse>> getSongs(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "songKey", required = false) String songKey,
            @RequestParam(name = "tag", required = false) String tag
    ) {
        return ResponseEntity.ok(songService.getSongs(query, songKey, tag));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        return ResponseEntity.ok(songService.getAllTags());
    }

    @PostMapping
    public ResponseEntity<SongResponse> createSong(@Valid @RequestBody SongRequest request) {
        SongResponse response = songService.createSong(request);
        return ResponseEntity
                .created(Objects.requireNonNull(URI.create("/api/songs/" + response.songId())))
                .body(response);
    }

    @GetMapping("/{songId}")
    public ResponseEntity<SongResponse> getSong(@PathVariable("songId") Long songId) {
        return ResponseEntity.ok(songService.getSong(songId));
    }

    @PutMapping("/{songId}")
    public ResponseEntity<SongResponse> updateSong(
            @PathVariable("songId") Long songId,
            @Valid @RequestBody SongRequest request
    ) {
        return ResponseEntity.ok(songService.updateSong(songId, request));
    }

    @PatchMapping("/{songId}/lyrics")
    public ResponseEntity<SongResponse> updateLyrics(
            @PathVariable("songId") Long songId,
            @RequestBody LyricsRequest request
    ) {
        return ResponseEntity.ok(songService.updateLyrics(songId, request.lyrics()));
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> deleteSong(@PathVariable("songId") Long songId) {
        songService.deleteSong(songId);
        return ResponseEntity.noContent().build();
    }
}
