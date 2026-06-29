package com.jeybell.sheetmusic.featurerequest;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRequestRepository extends JpaRepository<FeatureRequest, Long> {

    List<FeatureRequest> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<FeatureRequest> findByFeatureRequestIdAndDeletedAtIsNull(Long id);
}
