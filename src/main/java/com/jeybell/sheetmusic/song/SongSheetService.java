package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongSheetRequest;
import com.jeybell.sheetmusic.song.dto.SongSheetResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SongSheetService {

    private final SongRepository songRepository;
    private final SongSheetRepository songSheetRepository;
    private final SongFileService songFileService;

    public SongSheetService(
            SongRepository songRepository,
            SongSheetRepository songSheetRepository,
            SongFileService songFileService
    ) {
        this.songRepository = songRepository;
        this.songSheetRepository = songSheetRepository;
        this.songFileService = songFileService;
    }

    @Transactional
    public SongSheetResponse createSheet(Long songId, SongSheetRequest request) {
        Song song = getActiveSong(songId);

        SongSheet sheet = new SongSheet(
                request.sheetKey(),
                request.versionName(),
                request.memo()
        );
        song.addSheet(sheet);

        return SongSheetResponse.from(songSheetRepository.save(sheet));
    }

    public List<SongSheetResponse> getSheets(Long songId) {
        getActiveSong(songId);
        return songSheetRepository.findAllBySongSongIdAndDeletedAtIsNullOrderBySongSheetIdAsc(songId)
                .stream()
                .map(SongSheetResponse::from)
                .toList();
    }

    public SongSheetResponse getSheet(Long songSheetId) {
        return SongSheetResponse.from(getActiveSheet(songSheetId));
    }

    @Transactional
    public SongSheetResponse updateSheet(Long songSheetId, SongSheetRequest request) {
        SongSheet sheet = getActiveSheet(songSheetId);

        sheet.update(
                request.sheetKey(),
                request.versionName(),
                request.memo()
        );

        return SongSheetResponse.from(sheet);
    }

    @Transactional
    public void deleteSheet(Long songSheetId) {
        SongSheet sheet = getActiveSheet(songSheetId);
        songFileService.deleteFilesBySongSheetId(songSheetId);
        sheet.softDelete();
    }

    private Song getActiveSong(Long songId) {
        return songRepository.findActiveBySongIdWithSheets(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + songId));
    }

    private SongSheet getActiveSheet(Long songSheetId) {
        return songSheetRepository.findBySongSheetIdAndDeletedAtIsNull(songSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Song sheet not found: " + songSheetId));
    }
}
