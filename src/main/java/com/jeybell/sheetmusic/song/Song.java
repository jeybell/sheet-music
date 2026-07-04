package com.jeybell.sheetmusic.song;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "song_tags", joinColumns = @JoinColumn(name = "song_id"))
    @Column(name = "tag", length = 50)
    @Fetch(FetchMode.SUBSELECT)
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL)
    @OrderBy("sortNo asc, songSheetId asc")
    private List<SongSheet> sheets = new ArrayList<>();

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<SongLink> links = new ArrayList<>();

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
        this.tags = new ArrayList<>();
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

    public void updateTags(List<String> tags) {
        this.tags.clear();
        if (tags != null) {
            tags.stream()
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .distinct()
                .forEach(this.tags::add);
        }
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

    public void addLink(SongLink link) {
        this.links.add(link);
    }

    public List<SongLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 관리자 복구: soft delete 되돌리기. */
    public void restore() {
        this.deletedAt = null;
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

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
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
