package com.jeybell.sheetmusic.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "song_sheets")
public class SongSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "sheet_key", length = 20)
    private String sheetKey;

    @Column(name = "version_name", length = 100)
    private String versionName;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected SongSheet() {
    }

    public SongSheet(String sheetKey, String versionName, String memo) {
        this.sheetKey = sheetKey;
        this.versionName = versionName;
        this.memo = memo;
    }

    void assignSong(Song song) {
        this.song = song;
    }

    public void update(String sheetKey, String versionName, String memo) {
        this.sheetKey = sheetKey;
        this.versionName = versionName;
        this.memo = memo;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public Long getId() {
        return id;
    }

    public Song getSong() {
        return song;
    }

    public String getSheetKey() {
        return sheetKey;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getMemo() {
        return memo;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
