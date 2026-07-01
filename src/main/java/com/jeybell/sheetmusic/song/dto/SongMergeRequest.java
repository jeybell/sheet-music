package com.jeybell.sheetmusic.song.dto;

import java.util.List;

/**
 * 곡 병합 요청. sourceSongIds 곡들을 대상 곡으로 합친다.
 * dedupeSheets 가 true(기본)면 병합 후 키+파일이 동일한 악보 중복을 제거한다.
 */
public record SongMergeRequest(
        List<Long> sourceSongIds,
        Boolean dedupeSheets
) {
}
