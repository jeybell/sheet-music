package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.dto.PageResponse;
import com.jeybell.sheetmusic.song.dto.PopularSongResponse;
import com.jeybell.sheetmusic.global.exception.DuplicateTitleException;
import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongRequest;
import com.jeybell.sheetmusic.song.dto.SongResponse;
import com.jeybell.sheetmusic.song.dto.SongSheetRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;
    private final SongFileService songFileService;

    public SongService(SongRepository songRepository, SongFileService songFileService) {
        this.songRepository = songRepository;
        this.songFileService = songFileService;
    }

    public PageResponse<SongResponse> getSongs(String query, String songKey, List<String> tags,
                                               String sort, int page, int size) {
        String likeQuery = (query == null || query.isBlank())
                ? null
                : "%" + query.trim().toLowerCase() + "%";
        String normalizedKey = (songKey == null || songKey.isBlank())
                ? null
                : "%" + songKey.trim().toLowerCase() + "%";
        List<String> normalizedTags = (tags == null)
                ? List.of()
                : tags.stream().map(String::trim).filter(t -> !t.isBlank()).distinct().toList();
        long tagCount = normalizedTags.size();

        SongSort songSort = SongSort.from(sort);
        Page<Song> result = (songSort == SongSort.KEY)
                ? songRepository.searchSongsOrderByKey(
                        likeQuery, normalizedKey, normalizedTags, tagCount, PageRequest.of(page, size))
                : songRepository.searchSongs(
                        likeQuery, normalizedKey, normalizedTags, tagCount,
                        PageRequest.of(page, size, songSort.toSort()));

        return PageResponse.from(result.map(SongResponse::from));
    }

    public List<String> getAllTags() {
        return songRepository.findAllTags();
    }

    public List<PopularSongResponse> getPopularSongs(int limit) {
        return songRepository.findPopularSongs(PageRequest.of(0, limit));
    }

    @Transactional
    public SongResponse createSong(SongRequest request) {
        checkDuplicateTitle(request.title(), null);

        Song song = new Song(
                request.title(),
                request.artist(),
                null,
                request.memo(),
                request.youtubeUrl()
        );
        song.updateTags(request.tags());
        toSheets(request.sheets()).forEach(song::addSheet);

        return SongResponse.from(songRepository.save(song));
    }

    public SongResponse getSong(Long songId) {
        return SongResponse.from(getActiveSong(songId));
    }

    @Transactional
    public SongResponse updateSong(Long songId, SongRequest request) {
        checkDuplicateTitle(request.title(), songId);

        Song song = getActiveSong(songId);
        song.update(
                request.title(),
                request.artist(),
                null,
                request.memo(),
                request.youtubeUrl()
        );
        song.updateTags(request.tags());

        return SongResponse.from(song);
    }

    @Transactional
    public SongResponse updateLyrics(Long songId, String lyrics) {
        Song song = getActiveSong(songId);
        song.updateLyrics(lyrics == null || lyrics.isBlank() ? null : lyrics);
        return SongResponse.from(song);
    }

    @Transactional
    public void deleteSong(Long songId) {
        Song song = getActiveSong(songId);
        song.getSheets().stream()
                .filter(SongSheet::isActive)
                .forEach(sheet -> {
                    songFileService.deleteFilesBySongSheetId(sheet.getSongSheetId());
                    sheet.softDelete();
                });
        song.softDelete();
    }

    private void checkDuplicateTitle(String title, Long excludeId) {
        if (songRepository.existsByTitleIgnoreCase(title.trim(), excludeId)) {
            throw new DuplicateTitleException(title.trim());
        }
    }

    private Song getActiveSong(Long songId) {
        return songRepository.findActiveBySongIdWithSheets(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + songId));
    }

    private List<SongSheet> toSheets(List<SongSheetRequest> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
                .map(request -> new SongSheet(
                        request.sheetKey(),
                        request.versionName(),
                        request.memo()
                ))
                .toList();
    }
}
