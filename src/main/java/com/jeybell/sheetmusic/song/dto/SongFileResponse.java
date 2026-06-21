package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongFile;
import java.util.Arrays;
import java.util.List;

public record SongFileResponse(
        Long songFileId,
        Long songSheetId,
        String originalFileName,
        String storedFileName,
        String filePath,
        String contentType,
        Long fileSize,
        boolean ocrDone,
        OcrResult ocrResult
) {

    public static SongFileResponse from(SongFile songFile) {
        OcrResult ocr = null;
        if (songFile.isOcrDone()) {
            List<String> chords = songFile.getOcrChords() == null || songFile.getOcrChords().isBlank()
                    ? List.of()
                    : Arrays.asList(songFile.getOcrChords().split(","));
            ocr = new OcrResult(songFile.getOcrTitle(), songFile.getOcrKey(), chords, songFile.getOcrRawText());
        }
        return new SongFileResponse(
                songFile.getSongFileId(),
                songFile.getSongSheet().getSongSheetId(),
                songFile.getOriginalFileName(),
                songFile.getStoredFileName(),
                songFile.getFilePath(),
                songFile.getContentType(),
                songFile.getFileSize(),
                songFile.isOcrDone(),
                ocr
        );
    }
}
