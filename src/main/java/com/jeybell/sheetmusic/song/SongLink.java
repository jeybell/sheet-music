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

@Entity
@Table(name = "song_links")
public class SongLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_no", nullable = false)
    private int sortNo;

    protected SongLink() {}

    public SongLink(Song song, String title, String url, int sortNo) {
        this.song = song;
        this.title = title;
        this.url = url;
        this.sortNo = sortNo;
    }

    public void update(String title, String url, int sortNo) {
        this.title = title;
        this.url = url;
        this.sortNo = sortNo;
    }

    public Long getLinkId() { return linkId; }
    public Song getSong() { return song; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public int getSortNo() { return sortNo; }
}
