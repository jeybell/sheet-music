package com.jeybell.sheetmusic.featurerequest;

import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestCreateRequest;
import com.jeybell.sheetmusic.featurerequest.dto.FeatureRequestResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
