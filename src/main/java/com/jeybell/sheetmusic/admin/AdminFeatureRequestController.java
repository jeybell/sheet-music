package com.jeybell.sheetmusic.admin;

import com.jeybell.sheetmusic.featurerequest.FeatureRequestService;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestResponse;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 기능요청 관리 API. 목록 조회/등록은 일반 사용자용 {@code /api/feature-requests} 에 남기고,
 * 상태 변경과 삭제만 관리자로 제한한다.
 */
@RestController
@RequestMapping("/api/admin/feature-requests")
public class AdminFeatureRequestController {

    private final FeatureRequestService featureRequestService;

    public AdminFeatureRequestController(FeatureRequestService featureRequestService) {
        this.featureRequestService = featureRequestService;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FeatureRequestResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody FeatureRequestStatusRequest request
    ) {
        return ResponseEntity.ok(featureRequestService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        featureRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
