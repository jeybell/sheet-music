package com.jeybell.sheetmusic.admin;

import com.jeybell.sheetmusic.admin.dto.AdminDeletedSetlistResponse;
import com.jeybell.sheetmusic.admin.dto.AdminDeletedSongResponse;
import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.setlist.Setlist;
import com.jeybell.sheetmusic.setlist.SetlistRepository;
import com.jeybell.sheetmusic.song.Song;
import com.jeybell.sheetmusic.song.SongRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 콘텐츠(휴지통) 관리. soft delete 된 곡/콘티를 조회하고 복구한다.
 * 활성 콘텐츠의 일반 CRUD 는 기존 엔드포인트를 그대로 사용한다.
 */
@Service
@Transactional(readOnly = true)
public class AdminContentService {

    private final SongRepository songRepository;
    private final SetlistRepository setlistRepository;

    public AdminContentService(SongRepository songRepository, SetlistRepository setlistRepository) {
        this.songRepository = songRepository;
        this.setlistRepository = setlistRepository;
    }

    public List<AdminDeletedSongResponse> getDeletedSongs() {
        return songRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream()
                .map(AdminDeletedSongResponse::from)
                .toList();
    }

    @Transactional
    public void restoreSong(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("곡을 찾을 수 없습니다: " + songId));
        song.restore();
    }

    public List<AdminDeletedSetlistResponse> getDeletedSetlists() {
        return setlistRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream()
                .map(AdminDeletedSetlistResponse::from)
                .toList();
    }

    @Transactional
    public void restoreSetlist(Long setlistId) {
        Setlist setlist = setlistRepository.findById(setlistId)
                .orElseThrow(() -> new ResourceNotFoundException("콘티를 찾을 수 없습니다: " + setlistId));
        setlist.restore();
    }
}
