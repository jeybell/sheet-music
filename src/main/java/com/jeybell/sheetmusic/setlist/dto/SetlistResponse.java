package com.jeybell.sheetmusic.setlist.dto;

import com.jeybell.sheetmusic.setlist.Setlist;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SetlistResponse(
        Long setlistId,
        LocalDate serviceDate,
        String title,
        String memo,
        String youtubeUrl,
        String shareToken,
        List<SetlistItemResponse> items,
        LocalDateTime createdAt
) {

    public static SetlistResponse from(Setlist setlist) {
        return new SetlistResponse(
                setlist.getSetlistId(),
                setlist.getServiceDate(),
                setlist.getTitle(),
                setlist.getMemo(),
                setlist.getYoutubeUrl(),
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
        // 목록 조회(경량 프로젝션)는 YouTube 링크를 싣지 않는다. 링크는 상세 화면에서만 사용한다.
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
                        row.itemMemo(),
                        row.performanceKey(),
                        null
                ))
                .toList();
        return new SetlistResponse(
                head.setlistId(),
                head.serviceDate(),
                head.title(),
                head.memo(),
                null,
                head.shareToken(),
                items,
                head.createdAt()
        );
    }
}
