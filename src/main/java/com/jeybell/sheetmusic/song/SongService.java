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

    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    public List<SongResponse> getSongs(String songKey) {
        List<Song> songs = songKey == null || songKey.isBlank()
                ? songRepository.findAllActiveWithSheets()
                : songRepository.findAllActiveBySongKeyWithSheets(songKey);

        return songs
                .stream()
                .map(SongResponse::from)
                .toList();
    }

    @Transactional
    public SongResponse createSong(SongRequest request) {
        Song song = new Song(
                request.title(),
                request.artist(),
                request.memo()
        );
        toSheets(request.sheets()).forEach(song::addSheet);

        return SongResponse.from(songRepository.save(song));
    }

    public SongResponse getSong(Long id) {
        return SongResponse.from(getActiveSong(id));
    }

    @Transactional
    public SongResponse updateSong(Long id, SongRequest request) {
        Song song = getActiveSong(id);
        song.update(
                request.title(),
                request.artist(),
                request.memo()
        );

        return SongResponse.from(song);
    }

    @Transactional
    public void deleteSong(Long id) {
        Song song = getActiveSong(id);
        song.softDelete();
    }

    private Song getActiveSong(Long id) {
        return songRepository.findActiveByIdWithSheets(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + id));
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
