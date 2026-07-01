package com.jeybell.sheetmusic.setlist.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SetlistDuplicateRequest(
        @NotNull(message = "serviceDate is required")
        LocalDate serviceDate
) {
}
