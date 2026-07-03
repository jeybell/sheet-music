package com.jeybell.sheetmusic.song.dto;

import com.jeybell.sheetmusic.global.validation.SafeUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SongLinkRequest(
        @Size(max = 100) String title,
        @NotBlank @Size(max = 500) @SafeUrl String url
) {}
