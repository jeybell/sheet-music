package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.song.dto.SongFileAnnotationRequest;
import com.jeybell.sheetmusic.song.dto.SongFileAnnotationResponse;
import com.jeybell.sheetmusic.song.dto.SongFileDownloadResponse;
import com.jeybell.sheetmusic.song.dto.SongFileResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SongFileController {

    private final SongFileService songFileService;
    private final SongFileAnnotationService songFileAnnotationService;

    public SongFileController(
            SongFileService songFileService,
            SongFileAnnotationService songFileAnnotationService
    ) {
        this.songFileService = songFileService;
        this.songFileAnnotationService = songFileAnnotationService;
    }

    @PostMapping(path = "/api/song-sheets/{songSheetId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongFileResponse> uploadFile(
            @PathVariable("songSheetId") Long songSheetId,
            @RequestParam("file") MultipartFile file
    ) {
        SongFileResponse response = songFileService.uploadFile(songSheetId, file);
        return ResponseEntity
                .created(Objects.requireNonNull(URI.create("/api/song-files/" + response.songFileId() + "/download")))
                .body(response);
    }

    @GetMapping("/api/song-files/{songFileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable("songFileId") Long songFileId) {
        SongFileDownloadResponse response = songFileService.downloadFile(songFileId);
        String contentTypeValue = Objects.toString(response.contentType(), "");
        MediaType contentType = contentTypeValue.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : Objects.requireNonNull(MediaType.parseMediaType(contentTypeValue));

        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(contentType))
                .contentLength(response.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(response.originalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(response.resource());
    }

    @GetMapping("/api/song-files/{songFileId}/view")
    public ResponseEntity<Resource> viewFile(@PathVariable("songFileId") Long songFileId) {
        SongFileDownloadResponse response = songFileService.downloadFile(songFileId);
        String contentTypeValue = Objects.toString(response.contentType(), "");
        MediaType contentType = contentTypeValue.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : Objects.requireNonNull(MediaType.parseMediaType(contentTypeValue));

        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(contentType))
                .contentLength(response.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(response.originalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(response.resource());
    }

    @DeleteMapping("/api/song-files/{songFileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable("songFileId") Long songFileId) {
        songFileService.deleteFile(songFileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/song-files/{songFileId}/annotation")
    public ResponseEntity<SongFileAnnotationResponse> getAnnotation(@PathVariable("songFileId") Long songFileId) {
        return ResponseEntity.ok(songFileAnnotationService.getAnnotation(songFileId));
    }

    @PutMapping("/api/song-files/{songFileId}/annotation")
    public ResponseEntity<SongFileAnnotationResponse> saveAnnotation(
            @PathVariable("songFileId") Long songFileId,
            @Valid @RequestBody SongFileAnnotationRequest request
    ) {
        return ResponseEntity.ok(songFileAnnotationService.saveAnnotation(songFileId, request));
    }

    @DeleteMapping("/api/song-files/{songFileId}/annotation")
    public ResponseEntity<Void> deleteAnnotation(@PathVariable("songFileId") Long songFileId) {
        songFileAnnotationService.deleteAnnotation(songFileId);
        return ResponseEntity.noContent().build();
    }
}
