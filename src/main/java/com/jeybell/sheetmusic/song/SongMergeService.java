package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.setlist.SetlistItemRepository;
import com.jeybell.sheetmusic.song.dto.SongMergeResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중복 곡 병합. 원본 곡들의 활성 악보를 대상 곡으로 옮기고, 콘티 참조를 재지정한 뒤
 * 원본 곡을 soft delete 한다. dedupeSheets 면 키+파일이 동일한 악보 중복을 제거한다.
 * 파일은 스토리지에서 지우지 않고 DB soft delete 만 하여 복구 여지를 남긴다.
 */
@Service
public class SongMergeService {

    private final SongRepository songRepository;
    private final SongSheetRepository songSheetRepository;
    private final SetlistItemRepository setlistItemRepository;

    public SongMergeService(
            SongRepository songRepository,
            SongSheetRepository songSheetRepository,
            SetlistItemRepository setlistItemRepository
    ) {
        this.songRepository = songRepository;
        this.songSheetRepository = songSheetRepository;
        this.setlistItemRepository = setlistItemRepository;
    }

    @Transactional
    public SongMergeResponse merge(Long targetId, List<Long> sourceSongIds, boolean dedupeSheets) {
        Song target = getActiveSong(targetId);

        List<Long> sources = (sourceSongIds == null ? List.<Long>of() : sourceSongIds).stream()
                .filter(id -> id != null && !id.equals(targetId))
                .distinct()
                .toList();

        int movedSheets = 0;
        int redirectedItems = 0;
        int mergedSongs = 0;

        for (Long sourceId : sources) {
            Song source = getActiveSong(sourceId);
            for (SongSheet sheet : source.getSheets()) {
                if (sheet.getDeletedAt() == null) {
                    sheet.assignSong(target);
                    movedSheets++;
                }
            }
            redirectedItems += setlistItemRepository.redirectSongReferences(sourceId, targetId);
            source.softDelete();
            mergedSongs++;
        }

        int removedDuplicates = 0;
        if (dedupeSheets) {
            List<SongSheet> sheets =
                    songSheetRepository.findAllBySongSongIdAndDeletedAtIsNullOrderBySongSheetIdAsc(targetId);
            Map<String, SongSheet> seen = new HashMap<>();
            for (SongSheet sheet : sheets) {
                String signature = signatureOf(sheet);
                SongSheet kept = seen.get(signature);
                if (kept != null) {
                    redirectedItems += setlistItemRepository.redirectSheetReferences(
                            sheet.getSongSheetId(), kept.getSongSheetId());
                    sheet.softDelete();
                    removedDuplicates++;
                } else {
                    seen.put(signature, sheet);
                }
            }
        }

        return new SongMergeResponse(targetId, mergedSongs, movedSheets, removedDuplicates, redirectedItems);
    }

    /** 악보 서명 = 정규화한 키 + 활성 파일 원본명(정렬). 동일하면 중복으로 본다. */
    private String signatureOf(SongSheet sheet) {
        String key = sheet.getSheetKey() == null ? "" : sheet.getSheetKey().trim().toLowerCase();
        String files = sheet.getFiles().stream()
                .filter(file -> file.getDeletedAt() == null)
                .map(file -> file.getOriginalFileName() == null ? "" : file.getOriginalFileName())
                .sorted()
                .collect(Collectors.joining(","));
        return key + "|" + files;
    }

    private Song getActiveSong(Long songId) {
        return songRepository.findActiveBySongIdWithSheets(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + songId));
    }
}
