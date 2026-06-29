package com.jeybell.sheetmusic.featurerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeatureRequestCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @Size(max = 100) String author
) {}
