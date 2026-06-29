package com.jeybell.sheetmusic.featurerequest;

import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestCreateRequest;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestResponse;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestStatusRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feature-requests")
public class FeatureRequestController {

    private final FeatureRequestService service;

    public FeatureRequestController(FeatureRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FeatureRequestResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<FeatureRequestResponse> create(
            @Valid @RequestBody FeatureRequestCreateRequest request
    ) {
        FeatureRequestResponse response = service.create(request);
        return ResponseEntity
                .created(Objects.requireNonNull(
                        URI.create("/api/feature-requests/" + response.featureRequestId())))
                .body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FeatureRequestResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody FeatureRequestStatusRequest request
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
