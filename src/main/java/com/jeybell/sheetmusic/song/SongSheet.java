package com.jeybell.sheetmusic.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "song_sheets")
public class SongSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_sheet_id")
    private Long songSheetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "sheet_key", length = 20)
    private String sheetKey;

    @Column(name = "version_name", length = 100)
    private String versionName;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "sort_no", nullable = false)
    private int sortNo = 0;

    @OneToMany(mappedBy = "songSheet")
    private List<SongFile> files = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public void updateSortNo(int sortNo) {
        this.sortNo = sortNo;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public Long getSongSheetId() {
        return songSheetId;
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

    public int getSortNo() {
        return sortNo;
    }

    public List<SongFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
