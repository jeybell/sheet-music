package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.Song;
import java.time.OffsetDateTime;
import java.util.List;

public record SongResponse(
        Long id,
        String title,
        String artist,
        String memo,
        List<SongSheetResponse> sheets,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static SongResponse from(Song song) {
        return new SongResponse(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getMemo(),
                song.getSheets()
                        .stream()
                        .filter(sheet -> sheet.getDeletedAt() == null)
                        .map(SongSheetResponse::from)
                        .toList(),
                song.getCreatedBy(),
                song.getCreatedAt(),
                song.getUpdatedAt()
        );
    }
}
