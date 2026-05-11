package com.jeybell.sheetmusic.song;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String artist;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL)
    private List<SongSheet> sheets = new ArrayList<>();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Song() {
    }

    public Song(String title, String artist, String memo) {
        this.title = title;
        this.artist = artist;
        this.memo = memo;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(String title, String artist, String memo) {
        this.title = title;
        this.artist = artist;
        this.memo = memo;
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
        this.deletedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getMemo() {
        return memo;
    }

    public List<SongSheet> getSheets() {
        return Collections.unmodifiableList(sheets);
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
