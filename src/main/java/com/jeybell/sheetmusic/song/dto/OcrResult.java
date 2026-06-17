package com.jeybell.sheetmusic.song.dto;

import java.util.List;

public record OcrResult(
        String title,
        String key,
        List<String> chords,
        String rawText
) {}
