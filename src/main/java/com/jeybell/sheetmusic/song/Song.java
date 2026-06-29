package com.jeybell.sheetmusic.song;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_id")
    private Long songId;

    @Column(nullable = false)
    private String title;

    private String artist;

    private String composer;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(columnDefinition = "TEXT")
    private String lyrics;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL)
    private List<SongSheet> sheets = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Song() {
    }

    public Song(String title, String artist, String composer, String memo, String youtubeUrl) {
        this.title = title;
        this.artist = artist;
        this.composer = composer;
        this.memo = memo;
        this.lyrics = null;
        this.youtubeUrl = youtubeUrl;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String title, String artist, String composer, String memo, String youtubeUrl) {
        this.title = title;
        this.artist = artist;
        this.composer = composer;
        this.memo = memo;
        this.youtubeUrl = youtubeUrl;
    }

    public void updateLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public void replaceSheets(List<SongSheet> newSheets) {
        this.sheets.forEach(SongSheet::softDelete);
        newSheets.forEach(this::addSheet);
    }

    public void addSheet(SongSheet sheet) {
        sheet.assignSong(this);
        this.sheets.add(sheet);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public Long getSongId() {
        return songId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getComposer() {
        return composer;
    }

    public String getMemo() {
        return memo;
    }

    public String getLyrics() {
        return lyrics;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public List<SongSheet> getSheets() {
        return Collections.unmodifiableList(sheets);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
