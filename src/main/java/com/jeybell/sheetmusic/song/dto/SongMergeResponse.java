package com.jeybell.sheetmusic.song.dto;

/**
 * 곡 병합 결과 요약.
 */
public record SongMergeResponse(
        Long targetSongId,
        int mergedSongCount,
        int movedSheetCount,
        int removedDuplicateSheetCount,
        int redirectedSetlistItemCount
) {
}
