package com.jeybell.sheetmusic.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SongRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be 255 characters or less")
        String title,

        @Size(max = 255, message = "artist must be 255 characters or less")
        String artist,

        @Size(max = 20, message = "originalKey must be 20 characters or less")
        String originalKey,

        String memo
) {
}
