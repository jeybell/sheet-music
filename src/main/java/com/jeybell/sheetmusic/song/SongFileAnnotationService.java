package com.jeybell.sheetmusic.song;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongFileAnnotationRequest;
import com.jeybell.sheetmusic.song.dto.SongFileAnnotationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SongFileAnnotationService {

    private final SongFileRepository songFileRepository;
    private final SongFileAnnotationRepository annotationRepository;
    private final ObjectMapper objectMapper;

    public SongFileAnnotationService(
            SongFileRepository songFileRepository,
            SongFileAnnotationRepository annotationRepository,
            ObjectMapper objectMapper
    ) {
        this.songFileRepository = songFileRepository;
        this.annotationRepository = annotationRepository;
        this.objectMapper = objectMapper;
    }

    public SongFileAnnotationResponse getAnnotation(Long songFileId) {
        // 삭제(soft-delete)된 파일의 필기는 노출하지 않는다(saveAnnotation 과 동일한 활성 파일 기준).
        requireActiveFile(songFileId);
        return annotationRepository.findBySongFile_SongFileId(songFileId)
                .map(this::toResponse)
                .orElseGet(() -> new SongFileAnnotationResponse(songFileId, objectMapper.createArrayNode(), null));
    }

    @Transactional
    public SongFileAnnotationResponse saveAnnotation(Long songFileId, SongFileAnnotationRequest request) {
        SongFile songFile = requireActiveFile(songFileId);
        String strokesJson = writeStrokes(request.strokes());

        SongFileAnnotation annotation = annotationRepository.findBySongFile_SongFileId(songFileId)
                .orElse(null);
        if (annotation == null) {
            annotation = new SongFileAnnotation(songFile, strokesJson);
            annotationRepository.save(annotation);
        } else {
            annotation.updateStrokes(strokesJson);
        }
        return toResponse(annotation);
    }

    @Transactional
    public void deleteAnnotation(Long songFileId) {
        requireActiveFile(songFileId);
        annotationRepository.findBySongFile_SongFileId(songFileId)
                .ifPresent(annotationRepository::delete);
    }

    private SongFile requireActiveFile(Long songFileId) {
        return songFileRepository.findBySongFileIdAndDeletedAtIsNull(songFileId)
                .orElseThrow(() -> new ResourceNotFoundException("Song file not found: " + songFileId));
    }

    private SongFileAnnotationResponse toResponse(SongFileAnnotation annotation) {
        return new SongFileAnnotationResponse(
                annotation.getSongFile().getSongFileId(),
                readStrokes(annotation.getStrokes()),
                annotation.getUpdatedAt()
        );
    }

    private String writeStrokes(JsonNode strokes) {
        try {
            return objectMapper.writeValueAsString(strokes);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid strokes payload", e);
        }
    }

    private JsonNode readStrokes(String strokesJson) {
        try {
            return objectMapper.readTree(strokesJson);
        } catch (JsonProcessingException e) {
            return objectMapper.createArrayNode();
        }
    }
}
