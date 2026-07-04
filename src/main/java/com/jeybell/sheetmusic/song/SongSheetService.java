package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongSheetRequest;
import com.jeybell.sheetmusic.song.dto.SongSheetResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        // 새 버전은 맨 뒤로: 현재 활성 버전들의 최대 sortNo + 1
        int nextSortNo = song.getSheets().stream()
                .filter(SongSheet::isActive)
                .mapToInt(SongSheet::getSortNo)
                .max()
                .orElse(-1) + 1;
        sheet.updateSortNo(nextSortNo);
        song.addSheet(sheet);

        return SongSheetResponse.from(songSheetRepository.save(sheet));
    }

    /**
     * 악보 버전 순서 변경(드래그). 전달된 songSheetIds 순서대로 sortNo 를 0,1,2... 재부여한다.
     * 곡에 속하지 않거나 삭제된 악보 id 는 무시한다.
     */
    @Transactional
    public void reorderSheets(Long songId, List<Long> songSheetIds) {
        Song song = getActiveSong(songId);
        Map<Long, SongSheet> active = song.getSheets().stream()
                .filter(SongSheet::isActive)
                .collect(Collectors.toMap(SongSheet::getSongSheetId, s -> s));
        int order = 0;
        for (Long id : songSheetIds) {
            SongSheet sheet = active.get(id);
            if (sheet != null) {
                sheet.updateSortNo(order++);
            }
        }
    }

    public List<SongSheetResponse> getSheets(Long songId) {
        getActiveSong(songId);
        return songSheetRepository.findAllBySongSongIdAndDeletedAtIsNullOrderBySortNoAscSongSheetIdAsc(songId)
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
        // 키/버전이 바뀌면 이 악보 파일들의 표시 파일명(곡제목_키_버전)을 다시 생성한다.
        songFileService.regenerateFileNamesForSheet(sheet);

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
