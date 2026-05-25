package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongFileDownloadResponse;
import com.jeybell.sheetmusic.song.dto.SongFileResponse;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class SongFileService {

    private static final Path UPLOAD_ROOT = Path.of(".", "uploads", "sheets");

    private final SongSheetRepository songSheetRepository;
    private final SongFileRepository songFileRepository;

    public SongFileService(
            SongSheetRepository songSheetRepository,
            SongFileRepository songFileRepository
    ) {
        this.songSheetRepository = songSheetRepository;
        this.songFileRepository = songFileRepository;
    }

    @PostConstruct
    void createUploadRoot() {
        createDirectory(UPLOAD_ROOT);
    }

    @Transactional
    public SongFileResponse uploadFile(Long songSheetId, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        SongSheet songSheet = getActiveSongSheet(songSheetId);
        String originalFileName = cleanOriginalFileName(multipartFile.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + getExtension(originalFileName);
        Path sheetDirectory = UPLOAD_ROOT.resolve(String.valueOf(songSheetId)).normalize();
        Path destination = sheetDirectory.resolve(storedFileName).normalize();
        boolean fileStored = false;

        if (!destination.startsWith(sheetDirectory)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try {
            createDirectory(sheetDirectory);
            multipartFile.transferTo(destination);
            fileStored = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }

        SongFile songFile = new SongFile(
                songSheet,
                originalFileName,
                storedFileName,
                toStoredPath(destination),
                multipartFile.getContentType(),
                multipartFile.getSize()
        );

        try {
            return SongFileResponse.from(songFileRepository.save(songFile));
        } catch (RuntimeException exception) {
            if (fileStored) {
                deleteStoredFile(destination, exception);
            }
            throw exception;
        }
    }

    public SongFileDownloadResponse downloadFile(Long songFileId) {
        SongFile songFile = getActiveSongFile(songFileId);
        Path filePath = Path.of(songFile.getFilePath()).normalize();
        Resource resource = new PathResource(Objects.requireNonNull(filePath));

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Song file not found on disk: " + songFileId);
        }

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
        String cleanedFileName = StringUtils.cleanPath(
                originalFileName == null || originalFileName.isBlank() ? "file" : originalFileName
        );
        if (cleanedFileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return cleanedFileName;
    }

    private String getExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0) {
            return "";
        }
        return fileName.substring(extensionIndex);
    }

    private String toStoredPath(Path destination) {
        return "./" + destination.toString();
    }

    private void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create upload directory", exception);
        }
    }

    private void deleteStoredFile(Path destination, RuntimeException uploadException) {
        try {
            Files.deleteIfExists(destination);
        } catch (IOException exception) {
            uploadException.addSuppressed(exception);
        }
    }
}
