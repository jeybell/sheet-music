package com.jeybell.sheetmusic.setlist.dto;

import com.jeybell.sheetmusic.setlist.SetlistItem;
import com.jeybell.sheetmusic.song.SongFile;
import java.util.List;

public record SharedSetlistItemResponse(
        Long setlistItemId,
        Integer orderNo,
        Long songId,
        String songTitle,
        String songArtist,
        Long songSheetId,
        String sheetKey,
        String versionName,
        String memo,
        List<SharedFileResponse> files
) {

    public record SharedFileResponse(Long songFileId, String contentType, String originalFileName) {
        public static SharedFileResponse from(SongFile f) {
            return new SharedFileResponse(f.getSongFileId(), f.getContentType(), f.getOriginalFileName());
        }
    }

    public static SharedSetlistItemResponse from(SetlistItem item) {
        List<SharedFileResponse> files = item.getSongSheet() != null
                ? item.getSongSheet().getFiles().stream()
                        .filter(f -> f.getDeletedAt() == null)
                        .map(SharedFileResponse::from)
                        .toList()
                : List.of();

        return new SharedSetlistItemResponse(
                item.getSetlistItemId(),
                item.getOrderNo(),
                item.getSong().getSongId(),
                item.getSong().getTitle(),
                item.getSong().getArtist(),
                item.getSongSheet() != null ? item.getSongSheet().getSongSheetId() : null,
                item.getSongSheet() != null ? item.getSongSheet().getSheetKey() : null,
                item.getSongSheet() != null ? item.getSongSheet().getVersionName() : null,
                item.getMemo(),
                files
        );
    }
}
