package com.jeybell.sheetmusic.setlist.dto;

import jakarta.validation.constraints.NotNull;

public record SetlistItemUpdateRequest(
        Long songSheetId,

        @NotNull(message = "orderNo is required")
        Integer orderNo,

        String memo,

        String performanceKey
) {
}
