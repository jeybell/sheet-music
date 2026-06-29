package com.jeybell.sheetmusic.featurerequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "feature_requests")
public class FeatureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feature_request_id")
    private Long featureRequestId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeatureRequestStatus status = FeatureRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected FeatureRequest() {}

    public FeatureRequest(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(FeatureRequestStatus status) {
        this.status = status;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public Long getFeatureRequestId() { return featureRequestId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public FeatureRequestStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
