package com.jeybell.sheetmusic.global.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {

    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ErrorResponse(
                OffsetDateTime.now(),
                status,
                error,
                message,
                path,
                null
        );
    }

    public static ErrorResponse validation(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return new ErrorResponse(
                OffsetDateTime.now(),
                status,
                error,
                message,
                path,
                validationErrors
        );
    }
}
