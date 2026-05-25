package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongSheet;
import java.util.List;

public record SongSheetResponse(
        Long songSheetId,
        Long songId,
        String sheetKey,
        String versionName,
        String memo,
        List<SongFileResponse> files
) {

    public static SongSheetResponse from(SongSheet sheet) {
        return new SongSheetResponse(
                sheet.getSongSheetId(),
                sheet.getSong().getSongId(),
                sheet.getSheetKey(),
                sheet.getVersionName(),
                sheet.getMemo(),
                sheet.getFiles()
                        .stream()
                        .filter(file -> file.getDeletedAt() == null)
                        .map(SongFileResponse::from)
                        .toList()
        );
    }
}
