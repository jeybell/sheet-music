package com.jeybell.sheetmusic.song.dto;

/**
 * 자주 쓰는 곡(홈 대시보드용). 활성 콘티의 setlist_items 참조 횟수로 집계한다.
 */
public record PopularSongResponse(
        Long songId,
        String title,
        String artist,
        long usageCount
) {
}
