package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongRequest;
import com.jeybell.sheetmusic.song.dto.SongResponse;
import com.jeybell.sheetmusic.song.dto.SongSheetRequest;
import java.util.List;
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

    public List<SongResponse> getSongs(String query, String songKey) {
        String likeQuery = (query == null || query.isBlank())
                ? null
                : "%" + query.trim().toLowerCase() + "%";
        String normalizedKey = (songKey == null || songKey.isBlank()) ? null : songKey.trim();

        return songRepository.searchSongs(likeQuery, normalizedKey)
                .stream()
                .map(SongResponse::from)
                .toList();
    }

    @Transactional
    public SongResponse createSong(SongRequest request) {
        Song song = new Song(
                request.title(),
                request.artist(),
                request.composer(),
                request.memo()
        );
        toSheets(request.sheets()).forEach(song::addSheet);

        return SongResponse.from(songRepository.save(song));
    }

    public SongResponse getSong(Long songId) {
        return SongResponse.from(getActiveSong(songId));
    }

    @Transactional
    public SongResponse updateSong(Long songId, SongRequest request) {
        Song song = getActiveSong(songId);
        song.update(
                request.title(),
                request.artist(),
                request.composer(),
                request.memo()
        );

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
