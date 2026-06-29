package com.jeybell.sheetmusic.featurerequest.dto;

import com.jeybell.sheetmusic.featurerequest.FeatureRequest;
import com.jeybell.sheetmusic.featurerequest.FeatureRequestStatus;
import java.time.LocalDateTime;

public record FeatureRequestResponse(
        Long featureRequestId,
        String title,
        String content,
        String author,
        FeatureRequestStatus status,
        LocalDateTime createdAt
) {
    public static FeatureRequestResponse from(FeatureRequest fr) {
        return new FeatureRequestResponse(
                fr.getFeatureRequestId(),
                fr.getTitle(),
                fr.getContent(),
                fr.getAuthor(),
                fr.getStatus(),
                fr.getCreatedAt()
        );
    }
}
