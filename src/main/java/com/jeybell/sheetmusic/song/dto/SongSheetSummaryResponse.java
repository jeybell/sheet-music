package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongSheet;

public record SongSheetSummaryResponse(
        Long songSheetId,
        String sheetKey,
        String versionName,
        String memo
) {

    public static SongSheetSummaryResponse from(SongSheet sheet) {
        return new SongSheetSummaryResponse(
                sheet.getId(),
                sheet.getSheetKey(),
                sheet.getVersionName(),
                sheet.getMemo()
        );
    }
}
