package com.jeybell.sheetmusic.setlist;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import com.jeybell.sheetmusic.setlist.dto.SetlistRequest;
import com.jeybell.sheetmusic.setlist.dto.SetlistResponse;
import java.util.List;
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
        return setlistRepository.findAllActive()
                .stream()
                .map(SetlistResponse::from)
                .toList();
    }

    @Transactional
    public SetlistResponse createSetlist(SetlistRequest request) {
        Setlist setlist = new Setlist(
                request.serviceDate(),
                request.serviceType(),
                request.title(),
                request.memo()
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
                request.serviceType(),
                request.title(),
                request.memo()
        );
        return SetlistResponse.from(setlist);
    }

    @Transactional
    public void deleteSetlist(Long setlistId) {
        Setlist setlist = getActive(setlistId);
        setlist.softDelete();
    }

    private Setlist getActive(Long setlistId) {
        return setlistRepository.findActiveById(setlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Setlist not found: " + setlistId));
    }
}
