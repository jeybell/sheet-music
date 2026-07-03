package com.jeybell.sheetmusic.setlist.dto;

import com.jeybell.sheetmusic.global.validation.SafeUrl;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SetlistItemUpdateRequest(
        Long songSheetId,

        @NotNull(message = "orderNo is required")
        Integer orderNo,

        String memo,

        String performanceKey,

        @Size(max = 500, message = "youtubeUrl must be 500 characters or less")
        @SafeUrl
        String youtubeUrl
) {
}
