package com.jeybell.sheetmusic.setlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SetlistRequest(
        @NotNull(message = "serviceDate is required")
        LocalDate serviceDate,

        @Size(max = 255, message = "title must be 255 characters or less")
        String title,

        String memo,

        @Size(max = 500, message = "youtubeUrl must be 500 characters or less")
        String youtubeUrl
) {
}
