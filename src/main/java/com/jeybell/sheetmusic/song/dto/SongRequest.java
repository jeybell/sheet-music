package com.jeybell.sheetmusic.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record SongRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be 255 characters or less")
        String title,

        @Size(max = 255, message = "artist must be 255 characters or less")
        String artist,

        String memo,

        String lyrics,

        @Valid
        List<SongSheetRequest> sheets
) {
}
