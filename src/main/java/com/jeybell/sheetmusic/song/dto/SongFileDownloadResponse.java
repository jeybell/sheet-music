package com.jeybell.sheetmusic.song.dto;

import org.springframework.core.io.Resource;

public record SongFileDownloadResponse(
        String originalFileName,
        String contentType,
        Long fileSize,
        Resource resource
) {
}
