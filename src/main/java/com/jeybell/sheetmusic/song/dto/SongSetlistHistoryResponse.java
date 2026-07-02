package com.jeybell.sheetmusic.song.dto;

import java.time.LocalDate;

public record SongSetlistHistoryResponse(
        Long setlistId,
        LocalDate serviceDate,
        String title
) {
}
