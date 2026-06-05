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
                setlist.getItems().stream()
                        .map(SetlistItemResponse::from)
                        .toList(),
                setlist.getCreatedAt()
        );
    }
}
