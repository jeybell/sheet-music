package com.jeybell.sheetmusic.setlist.dto;

import jakarta.validation.constraints.NotNull;

public record SetlistItemRequest(
        @NotNull(message = "songId is required")
        Long songId,

        Long songSheetId,

        @NotNull(message = "orderNo is required")
        Integer orderNo,

        String memo,

        String performanceKey
) {
}
