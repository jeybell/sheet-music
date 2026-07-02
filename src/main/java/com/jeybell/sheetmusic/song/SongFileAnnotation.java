package com.jeybell.sheetmusic.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "song_file_annotations")
public class SongFileAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annotation_id")
    private Long annotationId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_file_id", nullable = false, unique = true)
    private SongFile songFile;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String strokes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SongFileAnnotation() {
    }

    public SongFileAnnotation(SongFile songFile, String strokes) {
        this.songFile = songFile;
        this.strokes = strokes;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateStrokes(String strokes) {
        this.strokes = strokes;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getAnnotationId() {
        return annotationId;
    }

    public SongFile getSongFile() {
        return songFile;
    }

    public String getStrokes() {
        return strokes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
