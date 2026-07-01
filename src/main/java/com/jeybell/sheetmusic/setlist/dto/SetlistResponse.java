package com.jeybell.sheetmusic.setlist.dto;

import com.jeybell.sheetmusic.setlist.Setlist;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SetlistResponse(
        Long setlistId,
        LocalDate serviceDate,
        String serviceType,
        String title,
        String memo,
        String shareToken,
        List<SetlistItemResponse> items,
        LocalDateTime createdAt
) {

    public static SetlistResponse from(Setlist setlist) {
        return new SetlistResponse(
                setlist.getSetlistId(),
                setlist.getServiceDate(),
                setlist.getServiceType(),
                setlist.getTitle(),
                setlist.getMemo(),
                setlist.getShareToken(),
                setlist.getItems().stream()
                        .map(SetlistItemResponse::from)
                        .toList(),
                setlist.getCreatedAt()
        );
    }

    /**
     * 하나의 콘티에 속한 평면 행들(SetlistListRow)을 하나의 SetlistResponse로 조립한다.
     * 아이템이 없는 콘티는 setlistItemId 가 null 인 행 하나만 존재하므로 빈 items 로 처리된다.
     */
    public static SetlistResponse fromRows(List<SetlistListRow> rows) {
        SetlistListRow head = rows.get(0);
        List<SetlistItemResponse> items = rows.stream()
                .filter(row -> row.setlistItemId() != null)
                .map(row -> new SetlistItemResponse(
                        row.setlistItemId(),
                        row.orderNo(),
                        row.songId(),
                        row.songTitle(),
                        row.songArtist(),
                        row.songSheetId(),
                        row.sheetKey(),
                        row.versionName(),
                        row.itemMemo()
                ))
                .toList();
        return new SetlistResponse(
                head.setlistId(),
                head.serviceDate(),
                head.serviceType(),
                head.title(),
                head.memo(),
                head.shareToken(),
                items,
                head.createdAt()
        );
    }
}
