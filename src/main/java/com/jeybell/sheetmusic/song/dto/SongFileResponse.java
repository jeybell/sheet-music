package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongFile;

public record SongFileResponse(
        Long songFileId,
        Long songSheetId,
        String originalFileName,
        String storedFileName,
        String filePath,
        String contentType,
        Long fileSize,
        OcrResult ocrResult
) {

    public static SongFileResponse from(SongFile songFile) {
        return from(songFile, null);
    }

    public static SongFileResponse from(SongFile songFile, OcrResult ocrResult) {
        return new SongFileResponse(
                songFile.getSongFileId(),
                songFile.getSongSheet().getSongSheetId(),
                songFile.getOriginalFileName(),
                songFile.getStoredFileName(),
                songFile.getFilePath(),
                songFile.getContentType(),
                songFile.getFileSize(),
                ocrResult
        );
    }
}
