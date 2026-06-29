package com.jeybell.sheetmusic.song;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.song.dto.SongLinkRequest;
import com.jeybell.sheetmusic.song.dto.SongLinkResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SongLinkService {

    private final SongRepository songRepository;
    private final SongLinkRepository songLinkRepository;

    public SongLinkService(SongRepository songRepository, SongLinkRepository songLinkRepository) {
        this.songRepository = songRepository;
        this.songLinkRepository = songLinkRepository;
    }

    @Transactional
    public SongLinkResponse addLink(Long songId, SongLinkRequest request) {
        Song song = getActiveSong(songId);
        int sortNo = song.getLinks().size();
        SongLink link = new SongLink(song, title(request), request.url(), sortNo);
        song.addLink(link);
        songLinkRepository.save(link);
        return SongLinkResponse.from(link);
    }

    @Transactional
    public SongLinkResponse updateLink(Long linkId, SongLinkRequest request) {
        SongLink link = getActiveLink(linkId);
        link.update(title(request), request.url(), link.getSortNo());
        return SongLinkResponse.from(link);
    }

    @Transactional
    public void deleteLink(Long linkId) {
        SongLink link = getActiveLink(linkId);
        songLinkRepository.delete(link);
    }

    private String title(SongLinkRequest request) {
        return (request.title() == null || request.title().isBlank()) ? "" : request.title().trim();
    }

    private Song getActiveSong(Long songId) {
        return songRepository.findActiveBySongIdWithSheets(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found: " + songId));
    }

    private SongLink getActiveLink(Long linkId) {
        return songLinkRepository.findByLinkIdAndSongDeletedAtIsNull(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + linkId));
    }
}
