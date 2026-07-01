package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.song.Song;
import com.jeybell.sheetmusic.song.SongSheet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "setlist_items")
public class SetlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setlist_item_id")
    private Long setlistItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setlist_id", nullable = false)
    private Setlist setlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_sheet_id")
    private SongSheet songSheet;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "performance_key", length = 20)
    private String performanceKey;

    protected SetlistItem() {
    }

    public SetlistItem(Song song, SongSheet songSheet, Integer orderNo, String memo) {
        this(song, songSheet, orderNo, memo, null);
    }

    public SetlistItem(Song song, SongSheet songSheet, Integer orderNo, String memo, String performanceKey) {
        this.song = song;
        this.songSheet = songSheet;
        this.orderNo = orderNo;
        this.memo = memo;
        this.performanceKey = performanceKey;
    }

    void assignSetlist(Setlist setlist) {
        this.setlist = setlist;
    }

    public void update(SongSheet songSheet, Integer orderNo, String memo, String performanceKey) {
        this.songSheet = songSheet;
        this.orderNo = orderNo;
        this.memo = memo;
        this.performanceKey = performanceKey;
    }

    public Long getSetlistItemId() {
        return setlistItemId;
    }

    public Setlist getSetlist() {
        return setlist;
    }

    public Song getSong() {
        return song;
    }

    public SongSheet getSongSheet() {
        return songSheet;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public String getMemo() {
        return memo;
    }

    public String getPerformanceKey() {
        return performanceKey;
    }
}
