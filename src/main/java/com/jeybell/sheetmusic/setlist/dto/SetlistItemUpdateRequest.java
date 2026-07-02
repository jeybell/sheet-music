package com.jeybell.sheetmusic.setlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SetlistItemUpdateRequest(
        Long songSheetId,

        @NotNull(message = "orderNo is required")
        Integer orderNo,

        String memo,

        String performanceKey,

        @Size(max = 500, message = "youtubeUrl must be 500 characters or less")
        String youtubeUrl
) {
}
