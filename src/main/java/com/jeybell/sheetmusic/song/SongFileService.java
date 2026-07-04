package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
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

    public SongFileService(
            SongSheetRepository songSheetRepository,
            SongFileRepository songFileRepository,
            StorageService storageService
    ) {
        this.songSheetRepository = songSheetRepository;
        this.songFileRepository = songFileRepository;
        this.storageService = storageService;
    }

    @Transactional
    public SongFileResponse uploadFile(Long songSheetId, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        SongSheet songSheet = getActiveSongSheet(songSheetId);
        Long songId = songSheet.getSong().getSongId();
        String contentType = multipartFile.getContentType();

        // 원본 업로드명은 확장자 추출에만 쓰고, 화면에 표시되는 파일명은
        // '곡제목_키.확장자'(키 없으면 '곡제목.확장자')로 새로 생성한다.
        String uploadedName = cleanOriginalFileName(multipartFile.getOriginalFilename());
        String extension = resolveExtension(uploadedName);
        int existingCount = songFileRepository
                .findAllBySongSheetSongSheetIdAndDeletedAtIsNull(songSheetId).size();
        String originalFileName = buildDisplayFileName(songSheet, extension, existingCount);
        String storedFileName = UUID.randomUUID() + extension;
        String key = "songs/" + songId + "/" + storedFileName;

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

    /** 마지막 경로 세그먼트의 확장자만 추출한다("a.png/b" 같은 입력에 슬래시가 섞이지 않도록). */
    private String resolveExtension(String fileName) {
        String ext = StringUtils.getFilenameExtension(fileName);
        return (ext == null || ext.isBlank()) ? "" : "." + ext;
    }

    /**
     * 화면에 표시할 파일명을 '곡제목_키.확장자' 형태로 만든다.
     * 키가 없으면 '곡제목.확장자', 같은 악보에 이미 파일이 있으면 순번(_2, _3...)을 붙인다.
     */
    private String buildDisplayFileName(SongSheet songSheet, String extension, int existingCount) {
        String title = sanitizeForFileName(songSheet.getSong().getTitle());
        if (title.isBlank()) {
            title = "악보";
        }
        String key = sanitizeForFileName(songSheet.getSheetKey());

        StringBuilder base = new StringBuilder(title);
        if (!key.isBlank()) {
            base.append("_").append(key);
        }
        if (existingCount > 0) {
            base.append("_").append(existingCount + 1);
        }
        return base + extension;
    }

    /** 파일명에 부적합한 문자(경로 구분자·제어문자 등)를 제거하고 앞뒤 공백/점을 정리한다. */
    private String sanitizeForFileName(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}
