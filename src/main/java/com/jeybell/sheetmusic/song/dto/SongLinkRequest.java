package com.jeybell.sheetmusic.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SongLinkRequest(
        @Size(max = 100) String title,
        @NotBlank @Size(max = 500) String url
) {}
