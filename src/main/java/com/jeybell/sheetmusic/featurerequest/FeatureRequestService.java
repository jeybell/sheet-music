package com.jeybell.sheetmusic.featurerequest;

import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestCreateRequest;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestResponse;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestStatusRequest;
import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureRequestService {

    private final FeatureRequestRepository repository;

    public FeatureRequestService(FeatureRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FeatureRequestResponse> getAll() {
        return repository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(FeatureRequestResponse::from)
                .toList();
    }

    @Transactional
    public FeatureRequestResponse create(FeatureRequestCreateRequest request) {
        FeatureRequest fr = new FeatureRequest(request.title(), request.content(), request.author());
        return FeatureRequestResponse.from(repository.save(fr));
    }

    @Transactional
    public FeatureRequestResponse updateStatus(Long id, FeatureRequestStatusRequest request) {
        FeatureRequest fr = repository.findByFeatureRequestIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("기능 요청을 찾을 수 없습니다."));
        fr.updateStatus(request.status());
        return FeatureRequestResponse.from(fr);
    }

    @Transactional
    public void delete(Long id) {
        FeatureRequest fr = repository.findByFeatureRequestIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("기능 요청을 찾을 수 없습니다."));
        fr.softDelete();
    }
}
