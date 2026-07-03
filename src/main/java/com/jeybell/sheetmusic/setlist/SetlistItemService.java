package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemResponse;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemUpdateRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistReorderRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.jeybell.sheetmusic.song.Song;
import com.jeybell.sheetmusic.song.SongRepository;
import com.jeybell.sheetmusic.song.SongSheet;
import com.jeybell.sheetmusic.song.SongSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SetlistItemService {

    private final SetlistRepository setlistRepository;
    private final SetlistItemRepository setlistItemRepository;
    private final SongRepository songRepository;
    private final SongSheetRepository songSheetRepository;

    public SetlistItemService(
            SetlistRepository setlistRepository,
            SetlistItemRepository setlistItemRepository,
            SongRepository songRepository,
            SongSheetRepository songSheetRepository
    ) {
        this.setlistRepository = setlistRepository;
        this.setlistItemRepository = setlistItemRepository;
        this.songRepository = songRepository;
        this.songSheetRepository = songSheetRepository;
    }

    @Transactional
    public SetlistItemResponse addItem(Long setlistId, SetlistItemRequest request) {
        Setlist setlist = getActiveSetlist(setlistId);
        Song song = getActiveSong(request.songId());
        SongSheet songSheet = resolveSheet(request.songSheetId(), song);

        SetlistItem item = new SetlistItem(song, songSheet, request.orderNo(), request.memo(),
                request.performanceKey(), request.youtubeUrl());
        setlist.addItem(item);
        // setlist.addItem()은 컬렉션에만 추가하므로(cascade persist 대기 상태),
        // IDENTITY 생성 전략인 setlistItemId가 응답 시점엔 아직 null이다.
        // 명시적으로 저장해 즉시 INSERT를 실행시켜 생성된 ID를 응답에 채운다.
        setlistItemRepository.save(item);

        return SetlistItemResponse.from(item);
    }

    @Transactional
    public SetlistItemResponse updateItem(Long itemId, SetlistItemUpdateRequest request) {
        SetlistItem item = getActiveItem(itemId);
        SongSheet songSheet = resolveSheetForUpdate(item, request.songSheetId());

        item.update(songSheet, request.orderNo(), request.memo(), request.performanceKey(), request.youtubeUrl());

        return SetlistItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        SetlistItem item = getActiveItem(itemId);
        setlistItemRepository.delete(item);
    }

    @Transactional
    public void reorderItems(Long setlistId, SetlistReorderRequest request) {
        List<Long> itemIds = request.itemIds();
        List<SetlistItem> items = setlistItemRepository.findAllByIdsAndSetlistActive(itemIds);

        Map<Long, SetlistItem> itemMap = items.stream()
                .collect(Collectors.toMap(SetlistItem::getSetlistItemId, i -> i));

        for (int i = 0; i < itemIds.size(); i++) {
            SetlistItem item = itemMap.get(itemIds.get(i));
            if (item != null && item.getSetlist().getSetlistId().equals(setlistId)) {
                item.update(item.getSongSheet(), i + 1, item.getMemo(), item.getPerformanceKey(), item.getYoutubeUrl());
            }
        }
    }

    private Setlist getActiveSetlist(Long setlistId) {
        return setlistRepository.findActiveById(setlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Setlist not found: " + setlistId));
    }

    private Song getActiveSong(Long songId) {
        return songRepository.findActiveBySongIdWithSheets(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + songId));
    }

    private SetlistItem getActiveItem(Long itemId) {
        return setlistItemRepository.findBySetlistItemIdAndSetlistDeletedAtIsNull(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Setlist item not found: " + itemId));
    }

    /**
     * 수정 시 악보 참조 결정.
     * 요청한 songSheetId 가 아이템의 현재 값과 동일하면(= 악보 변경 없음) 소유권을 재검증하지 않고
     * 기존 참조를 그대로 유지한다. 과거 곡 병합/중복 정리(#104)로 song_sheet_id 가 다른 곡 소속이거나
     * soft-delete 된 시트를 가리키는 스테일 상태가 되더라도, memo/연주키/youtube 인라인 수정은 막지 않기 위함.
     * 악보를 실제로 다른 값으로 바꾸는 경우에만 {@link #resolveSheet}로 소유권을 검증한다(위반 시 400).
     */
    private SongSheet resolveSheetForUpdate(SetlistItem item, Long requestedSongSheetId) {
        SongSheet current = item.getSongSheet();
        Long currentSongSheetId = current == null ? null : current.getSongSheetId();
        if (Objects.equals(requestedSongSheetId, currentSongSheetId)) {
            return current;
        }
        return resolveSheet(requestedSongSheetId, item.getSong());
    }

    private SongSheet resolveSheet(Long songSheetId, Song song) {
        if (songSheetId == null) {
            return null;
        }
        SongSheet sheet = songSheetRepository.findBySongSheetIdAndDeletedAtIsNull(songSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Song sheet not found: " + songSheetId));

        if (!sheet.getSong().getSongId().equals(song.getSongId())) {
            throw new IllegalArgumentException(
                    "Song sheet " + songSheetId + " does not belong to song " + song.getSongId()
            );
        }
        return sheet;
    }
}
