package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.song.SongSheet;
import java.util.List;

public record SongSheetSummaryResponse(
        Long songSheetId,
        String sheetKey,
        String versionName,
        String memo,
        List<SongFileResponse> files
) {

    public static SongSheetSummaryResponse from(SongSheet sheet) {
        return new SongSheetSummaryResponse(
                sheet.getSongSheetId(),
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
