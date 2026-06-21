package com.jeybell.sheetmusic.ocr;

import com.jeybell.sheetmusic.song.SongFile;
import com.jeybell.sheetmusic.song.SongFileRepository;
import com.jeybell.sheetmusic.song.dto.OcrResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncOcrService {

    private static final Logger log = LoggerFactory.getLogger(AsyncOcrService.class);

    private final OcrService ocrService;
    private final SongFileRepository songFileRepository;

    public AsyncOcrService(OcrService ocrService, SongFileRepository songFileRepository) {
        this.ocrService = ocrService;
        this.songFileRepository = songFileRepository;
    }

    @Async
    @Transactional
    public void analyzeAndSave(Long songFileId, byte[] imageBytes, String fileName) {
        OcrResult result = ocrService.analyze(imageBytes, fileName);
        if (result == null) {
            log.info("OCR 결과 없음 (songFileId={})", songFileId);
            return;
        }
        songFileRepository.findById(songFileId).ifPresent(file -> {
            String chords = result.chords() == null ? "" : String.join(",", result.chords());
            file.applyOcrResult(result.title(), result.key(), chords, result.rawText());
            log.info("OCR 저장 완료 (songFileId={}, title={})", songFileId, result.title());
        });
    }
}
