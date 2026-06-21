package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.ocr.AsyncOcrService;
import com.jeybell.sheetmusic.song.dto.SongFileDownloadResponse;
import com.jeybell.sheetmusic.song.dto.SongFileResponse;
import com.jeybell.sheetmusic.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class SongFileService {

    private final SongSheetRepository songSheetRepository;
    private final SongFileRepository songFileRepository;
    private final StorageService storageService;
    private final AsyncOcrService asyncOcrService;

    public SongFileService(
            SongSheetRepository songSheetRepository,
            SongFileRepository songFileRepository,
            StorageService storageService,
            AsyncOcrService asyncOcrService
    ) {
        this.songSheetRepository = songSheetRepository;
        this.songFileRepository = songFileRepository;
        this.storageService = storageService;
        this.asyncOcrService = asyncOcrService;
    }

    @Transactional
    public SongFileResponse uploadFile(Long songSheetId, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        SongSheet songSheet = getActiveSongSheet(songSheetId);
        String contentType = multipartFile.getContentType();
        String originalFileName = cleanOriginalFileName(multipartFile.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + getExtension(originalFileName);
        String key = "sheets/" + songSheetId + "/" + storedFileName;

        byte[] fileBytes;
        try {
            fileBytes = multipartFile.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read upload stream", e);
        }

        storageService.store(key, new ByteArrayInputStream(fileBytes), multipartFile.getSize(), contentType);

        SongFile songFile = new SongFile(
                songSheet,
                originalFileName,
                storedFileName,
                key,
                contentType,
                multipartFile.getSize()
        );

        try {
            songFile = songFileRepository.save(songFile);
        } catch (RuntimeException e) {
            storageService.delete(key);
            throw e;
        }

        if (isImageContentType(contentType)) {
            asyncOcrService.analyzeAndSave(songFile.getSongFileId(), fileBytes, originalFileName);
        }

        return SongFileResponse.from(songFile);
    }

    public SongFileDownloadResponse downloadFile(Long songFileId) {
        SongFile songFile = getActiveSongFile(songFileId);
        Resource resource = storageService.load(songFile.getFilePath());

        return new SongFileDownloadResponse(
                songFile.getOriginalFileName(),
                songFile.getContentType(),
                songFile.getFileSize(),
                resource
        );
    }

    @Transactional
    public void deleteFile(Long songFileId) {
        SongFile songFile = getActiveSongFile(songFileId);
        songFile.softDelete();
        storageService.delete(songFile.getFilePath());
    }

    @Transactional
    public void deleteFilesBySongSheetId(Long songSheetId) {
        List<SongFile> files = songFileRepository.findAllBySongSheetSongSheetIdAndDeletedAtIsNull(songSheetId);
        for (SongFile file : files) {
            file.softDelete();
            storageService.delete(file.getFilePath());
        }
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private SongSheet getActiveSongSheet(Long songSheetId) {
        return songSheetRepository.findBySongSheetIdAndDeletedAtIsNull(songSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Song sheet not found: " + songSheetId));
    }

    private SongFile getActiveSongFile(Long songFileId) {
        return songFileRepository.findBySongFileIdAndDeletedAtIsNull(songFileId)
                .orElseThrow(() -> new ResourceNotFoundException("Song file not found: " + songFileId));
    }

    private String cleanOriginalFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(
                originalFileName == null || originalFileName.isBlank() ? "file" : originalFileName
        );
        if (cleaned.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return cleaned;
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx);
    }
}
