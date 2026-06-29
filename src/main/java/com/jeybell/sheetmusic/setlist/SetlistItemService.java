package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemResponse;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemUpdateRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistReorderRequest;
import java.util.List;
import java.util.Map;
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

        SetlistItem item = new SetlistItem(song, songSheet, request.orderNo(), request.memo());
        setlist.addItem(item);

        return SetlistItemResponse.from(item);
    }

    @Transactional
    public SetlistItemResponse updateItem(Long itemId, SetlistItemUpdateRequest request) {
        SetlistItem item = getActiveItem(itemId);
        SongSheet songSheet = resolveSheet(request.songSheetId(), item.getSong());

        item.update(songSheet, request.orderNo(), request.memo());

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
                item.update(item.getSongSheet(), i + 1, item.getMemo());
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
