package com.jeybell.sheetmusic.song.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record SongFileAnnotationResponse(
        Long songFileId,
        JsonNode strokes,
        LocalDateTime updatedAt
) {
}
