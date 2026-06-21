package com.jeybell.sheetmusic.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "song_files")
public class SongFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_file_id")
    private Long songFileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_sheet_id", nullable = false)
    private SongSheet songSheet;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "ocr_title")
    private String ocrTitle;

    @Column(name = "ocr_key", length = 20)
    private String ocrKey;

    @Column(name = "ocr_chords", columnDefinition = "TEXT")
    private String ocrChords;

    @Column(name = "ocr_raw_text", columnDefinition = "TEXT")
    private String ocrRawText;

    @Column(name = "ocr_done", nullable = false)
    private boolean ocrDone = false;

    protected SongFile() {
    }

    public SongFile(
            SongSheet songSheet,
            String originalFileName,
            String storedFileName,
            String filePath,
            String contentType,
            Long fileSize
    ) {
        this.songSheet = songSheet;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public Long getSongFileId() {
        return songFileId;
    }

    public SongSheet getSongSheet() {
        return songSheet;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void applyOcrResult(String title, String key, String chords, String rawText) {
        this.ocrTitle = title;
        this.ocrKey = key;
        this.ocrChords = chords;
        this.ocrRawText = rawText;
        this.ocrDone = true;
    }

    public String getOcrTitle() { return ocrTitle; }
    public String getOcrKey() { return ocrKey; }
    public String getOcrChords() { return ocrChords; }
    public String getOcrRawText() { return ocrRawText; }
    public boolean isOcrDone() { return ocrDone; }
}
