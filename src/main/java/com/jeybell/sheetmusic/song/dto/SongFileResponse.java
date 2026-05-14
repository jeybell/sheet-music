package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongFile;

public record SongFileResponse(
        Long id,
        Long songSheetId,
        String originalFileName,
        String storedFileName,
        String filePath,
        String contentType,
        Long fileSize
) {

    public static SongFileResponse from(SongFile songFile) {
        return new SongFileResponse(
                songFile.getId(),
                songFile.getSongSheet().getId(),
                songFile.getOriginalFileName(),
                songFile.getStoredFileName(),
                songFile.getFilePath(),
                songFile.getContentType(),
                songFile.getFileSize()
        );
    }
}
