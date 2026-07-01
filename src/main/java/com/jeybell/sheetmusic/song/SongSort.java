package com.jeybell.sheetmusic.song;

import org.springframework.data.domain.Sort;

/**
 * 곡 목록 정렬 기준.
 * KEY(키순)는 키가 자식 엔티티(song_sheets)에 있어 루트 필드 Sort 로 표현할 수 없으므로
 * 전용 쿼리에서 서브쿼리 정렬로 처리한다(여기서는 unsorted 로 둔다).
 */
public enum SongSort {

    LATEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    TITLE(Sort.by(Sort.Direction.ASC, "title")),
    ARTIST(Sort.by(Sort.Direction.ASC, "artist")),
    KEY(Sort.unsorted());

    private final Sort sort;

    SongSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    /** 잘못된/누락된 값은 기본값 TITLE(이름순)로 폴백한다. */
    public static SongSort from(String value) {
        if (value == null) {
            return TITLE;
        }
        try {
            return SongSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TITLE;
        }
    }
}
