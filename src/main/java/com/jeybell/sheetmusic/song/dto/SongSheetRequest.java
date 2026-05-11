package com.jeybell.sheetmusic.song.dto;

import jakarta.validation.constraints.Size;

public record SongSheetRequest(
        @Size(max = 20, message = "sheetKey must be 20 characters or less")
        String sheetKey,

        @Size(max = 100, message = "versionName must be 100 characters or less")
        String versionName,

        String memo
) {
}
