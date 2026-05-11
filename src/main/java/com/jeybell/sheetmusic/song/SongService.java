package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongRequest;
import com.jeybell.sheetmusic.song.dto.SongResponse;
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

    public List<SongResponse> getSongs() {
        return songRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(SongResponse::from)
                .toList();
    }

    @Transactional
    public SongResponse createSong(SongRequest request) {
        Song song = new Song(
                request.title(),
                request.artist(),
                request.originalKey(),
                request.memo()
        );

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
                request.originalKey(),
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
        return songRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + id));
    }
}
