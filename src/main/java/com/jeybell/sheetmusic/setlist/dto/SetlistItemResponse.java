package com.jeybell.sheetmusic.setlist.dto;

import com.jeybell.sheetmusic.setlist.SetlistItem;

public record SetlistItemResponse(
        Long setlistItemId,
        Integer orderNo,
        Long songId,
        String songTitle,
        String songArtist,
        Long songSheetId,
        String sheetKey,
        String versionName,
        String memo
) {

    public static SetlistItemResponse from(SetlistItem item) {
        return new SetlistItemResponse(
                item.getSetlistItemId(),
                item.getOrderNo(),
                item.getSong().getSongId(),
                item.getSong().getTitle(),
                item.getSong().getArtist(),
                item.getSongSheet() != null ? item.getSongSheet().getSongSheetId() : null,
                item.getSongSheet() != null ? item.getSongSheet().getSheetKey() : null,
                item.getSongSheet() != null ? item.getSongSheet().getVersionName() : null,
                item.getMemo()
        );
    }
}
