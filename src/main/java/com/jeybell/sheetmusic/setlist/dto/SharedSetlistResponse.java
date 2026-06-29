package com.jeybell.sheetmusic.setlist.dto;

import com.jeybell.sheetmusic.setlist.Setlist;
import java.time.LocalDate;
import java.util.List;

public record SharedSetlistResponse(
        Long setlistId,
        LocalDate serviceDate,
        String serviceType,
        String title,
        String memo,
        List<SharedSetlistItemResponse> items
) {

    public static SharedSetlistResponse from(Setlist setlist) {
        return new SharedSetlistResponse(
                setlist.getSetlistId(),
                setlist.getServiceDate(),
                setlist.getServiceType(),
                setlist.getTitle(),
                setlist.getMemo(),
                setlist.getItems().stream()
                        .map(SharedSetlistItemResponse::from)
                        .toList()
        );
    }
}
