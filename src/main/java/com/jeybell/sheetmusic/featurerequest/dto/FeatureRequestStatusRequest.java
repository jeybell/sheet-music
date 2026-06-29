package com.jeybell.sheetmusic.featurerequest.dto;

import com.jeybell.sheetmusic.featurerequest.FeatureRequestStatus;
import jakarta.validation.constraints.NotNull;

public record FeatureRequestStatusRequest(
        @NotNull FeatureRequestStatus status
) {}
