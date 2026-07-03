package com.jeybell.sheetmusic.admin.dto;

import com.jeybell.sheetmusic.song.Song;
import java.time.LocalDateTime;

public record AdminDeletedSongResponse(
        Long songId,
        String title,
        String artist,
        LocalDateTime deletedAt
) {
    public static AdminDeletedSongResponse from(Song song) {
        return new AdminDeletedSongResponse(
                song.getSongId(),
                song.getTitle(),
                song.getArtist(),
                song.getDeletedAt()
        );
    }
}
