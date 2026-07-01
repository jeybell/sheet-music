package com.jeybell.sheetmusic.global.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 응답 공통 포맷.
 * 무한 스크롤/총 건수 표시에 필요한 최소 메타(총 건수, 다음 페이지 여부)를 담는다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
