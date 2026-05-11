package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongSheet;

public record SongSheetResponse(
        Long id,
        Long songId,
        String sheetKey,
        String versionName,
        String memo
) {

    public static SongSheetResponse from(SongSheet sheet) {
        return new SongSheetResponse(
                sheet.getId(),
                sheet.getSong().getId(),
                sheet.getSheetKey(),
                sheet.getVersionName(),
                sheet.getMemo()
        );
    }
}
