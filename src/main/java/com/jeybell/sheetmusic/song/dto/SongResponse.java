package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.Song;
import java.time.LocalDateTime;
import java.util.List;

public record SongResponse(
        Long songId,
        String title,
        String artist,
        String composer,
        String memo,
        List<SongSheetSummaryResponse> songSheets,
        LocalDateTime createdAt
) {

    public static SongResponse from(Song song) {
        return new SongResponse(
                song.getSongId(),
                song.getTitle(),
                song.getArtist(),
                song.getComposer(),
                song.getMemo(),
                song.getSheets()
                        .stream()
                        .filter(sheet -> sheet.getDeletedAt() == null)
                        .map(SongSheetSummaryResponse::from)
                        .toList(),
                song.getCreatedAt()
        );
    }
}
