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
import org.springframework.http.InvalidMediaTypeException;
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
        return ResponseEntity.ok()
                .contentType(resolveContentType(response.contentType()))
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
        return ResponseEntity.ok()
                .contentType(resolveContentType(response.contentType()))
                .contentLength(response.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(response.originalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(response.resource());
    }

    /**
     * 저장된 Content-Type 을 안전하게 파싱한다. 업로드 시 비정상 헤더가 들어와도
     * 파싱 실패 시 application/octet-stream 으로 폴백해 다운로드/뷰가 영구 500 나는 걸 막는다.
     */
    private MediaType resolveContentType(String contentTypeValue) {
        String value = Objects.toString(contentTypeValue, "");
        if (value.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(value);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @DeleteMapping("/api/song-files/{songFileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable("songFileId") Long songFileId) {
        songFileService.deleteFile(songFileId);
        return ResponseEntity.noContent().build();
    }

    /** 밝기/대비 보정 등으로 편집한 이미지를 같은 파일(songFileId)의 내용으로 교체 저장한다. */
    @PutMapping(path = "/api/song-files/{songFileId}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongFileResponse> replaceContent(
            @PathVariable("songFileId") Long songFileId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(songFileService.replaceContent(songFileId, file));
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
