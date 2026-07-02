package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.setlist.dto.SetlistListRow;
import com.jeybell.sheetmusic.setlist.dto.SetlistRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistResponse;
import com.jeybell.sheetmusic.setlist.dto.SharedSetlistResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SetlistService {

    private final SetlistRepository setlistRepository;

    public SetlistService(SetlistRepository setlistRepository) {
        this.setlistRepository = setlistRepository;
    }

    public List<SetlistResponse> getSetlists() {
        Map<Long, List<SetlistListRow>> grouped = new LinkedHashMap<>();
        for (SetlistListRow row : setlistRepository.findAllActiveForList()) {
            grouped.computeIfAbsent(row.setlistId(), key -> new ArrayList<>()).add(row);
        }
        return grouped.values().stream()
                .map(SetlistResponse::fromRows)
                .toList();
    }

    @Transactional
    public SetlistResponse createSetlist(SetlistRequest request) {
        Setlist setlist = new Setlist(
                request.serviceDate(),
                request.title(),
                request.memo(),
                request.youtubeUrl()
        );
        return SetlistResponse.from(setlistRepository.save(setlist));
    }

    public SetlistResponse getSetlist(Long setlistId) {
        return SetlistResponse.from(getActive(setlistId));
    }

    @Transactional
    public SetlistResponse updateSetlist(Long setlistId, SetlistRequest request) {
        Setlist setlist = getActive(setlistId);
        setlist.update(
                request.serviceDate(),
                request.title(),
                request.memo(),
                request.youtubeUrl()
        );
        return SetlistResponse.from(setlist);
    }

    @Transactional
    public void deleteSetlist(Long setlistId) {
        Setlist setlist = getActive(setlistId);
        setlist.softDelete();
    }

    /**
     * 콘티 복사(템플릿으로 재사용). 곡 순서·악보 버전 설정을 그대로 복사하고
     * 날짜만 새로 지정한다. 공유 링크는 복사하지 않는다.
     */
    @Transactional
    public SetlistResponse duplicateSetlist(Long setlistId, LocalDate newServiceDate) {
        Setlist source = getActive(setlistId);
        Setlist copy = new Setlist(newServiceDate, source.getTitle(), source.getMemo(), source.getYoutubeUrl());
        for (SetlistItem item : source.getItems()) {
            copy.addItem(new SetlistItem(item.getSong(), item.getSongSheet(), item.getOrderNo(),
                    item.getMemo(), item.getPerformanceKey(), item.getYoutubeUrl()));
        }
        return SetlistResponse.from(setlistRepository.save(copy));
    }

    @Transactional
    public String generateShareToken(Long setlistId) {
        Setlist setlist = getActive(setlistId);
        if (setlist.getShareToken() != null) {
            return setlist.getShareToken();
        }
        return setlist.generateShareToken();
    }

    @Transactional
    public void revokeShareToken(Long setlistId) {
        Setlist setlist = getActive(setlistId);
        setlist.revokeShareToken();
    }

    @Transactional(readOnly = true)
    public SharedSetlistResponse getByShareToken(String token) {
        Setlist setlist = setlistRepository.findByShareToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("공유 링크를 찾을 수 없습니다."));
        return SharedSetlistResponse.from(setlist);
    }

    private Setlist getActive(Long setlistId) {
        return setlistRepository.findActiveById(setlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Setlist not found: " + setlistId));
    }
}
