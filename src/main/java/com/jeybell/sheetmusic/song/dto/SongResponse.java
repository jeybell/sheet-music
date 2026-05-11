package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.Song;
import java.time.OffsetDateTime;

public record SongResponse(
        Long id,
        String title,
        String artist,
        String originalKey,
        String memo,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static SongResponse from(Song song) {
        return new SongResponse(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getOriginalKey(),
                song.getMemo(),
                song.getCreatedBy(),
                song.getCreatedAt(),
                song.getUpdatedAt()
        );
    }
}
