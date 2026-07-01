package com.jeybell.sheetmusic.setlist.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 콘티 목록 전용 경량 프로젝션 (평면 행).
 * Song 엔티티를 로드하지 않아 EAGER tags/links 서브셀렉트가 발생하지 않는다.
 * 아이템이 없는 콘티는 아이템 관련 필드가 모두 null 인 단일 행으로 조회된다.
 */
public record SetlistListRow(
        Long setlistId,
        LocalDate serviceDate,
        String serviceType,
        String title,
        String memo,
        String shareToken,
        LocalDateTime createdAt,
        Long setlistItemId,
        Integer orderNo,
        Long songId,
        String songTitle,
        String songArtist,
        Long songSheetId,
        String sheetKey,
        String versionName,
        String itemMemo
) {
}
