package com.jeybell.sheetmusic.admin.dto;

import com.jeybell.sheetmusic.setlist.Setlist;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminDeletedSetlistResponse(
        Long setlistId,
        String title,
        LocalDate serviceDate,
        LocalDateTime deletedAt
) {
    public static AdminDeletedSetlistResponse from(Setlist setlist) {
        return new AdminDeletedSetlistResponse(
                setlist.getSetlistId(),
                setlist.getTitle(),
                setlist.getServiceDate(),
                setlist.getDeletedAt()
        );
    }
}
