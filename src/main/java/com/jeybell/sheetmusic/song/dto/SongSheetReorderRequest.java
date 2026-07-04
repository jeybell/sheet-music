package com.jeybell.sheetmusic.song.dto;

import java.util.List;

/** 악보 버전 순서 변경 요청. songSheetIds 순서대로 정렬 순번을 재부여한다. */
public record SongSheetReorderRequest(List<Long> songSheetIds) {}
