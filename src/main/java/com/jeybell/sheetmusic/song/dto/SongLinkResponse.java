package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongLink;

public record SongLinkResponse(Long linkId, String title, String url) {
    public static SongLinkResponse from(SongLink link) {
        return new SongLinkResponse(link.getLinkId(), link.getTitle(), link.getUrl());
    }
}
