package com.jeybell.sheetmusic.setlist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "setlists")
public class Setlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setlist_id")
    private Long setlistId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "service_type", length = 50)
    private String serviceType;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "setlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<SetlistItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Setlist() {
    }

    public Setlist(LocalDate serviceDate, String serviceType, String title, String memo) {
        this.serviceDate = serviceDate;
        this.serviceType = serviceType;
        this.title = title;
        this.memo = memo;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(LocalDate serviceDate, String serviceType, String title, String memo) {
        this.serviceDate = serviceDate;
        this.serviceType = serviceType;
        this.title = title;
        this.memo = memo;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void addItem(SetlistItem item) {
        item.assignSetlist(this);
        this.items.add(item);
    }

    public Long getSetlistId() {
        return setlistId;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getTitle() {
        return title;
    }

    public String getMemo() {
        return memo;
    }

    public List<SetlistItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
