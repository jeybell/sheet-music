package com.jeybell.sheetmusic.song.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record SongFileAnnotationRequest(
        @NotNull(message = "strokes is required")
        JsonNode strokes
) {
}
