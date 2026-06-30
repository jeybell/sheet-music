package com.jeybell.sheetmusic.storage;

import com.jeybell.sheetmusic.global.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path basePath;

    public LocalStorageService(StorageProperties properties) {
        this.basePath = Path.of(properties.local().basePath()).normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage base directory", e);
        }
    }

    @Override
    public void store(String key, InputStream inputStream, long size, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + key, e);
        }
    }

    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        Resource resource = new PathResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("File not found: " + key);
        }
        return resource;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Failed to delete local file: {}", key, e);
        }
    }

    @Override
    public void copy(String sourceKey, String destKey) {
        Path src = resolve(sourceKey);
        Path dst = resolve(destKey);
        try {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy file: " + sourceKey + " → " + destKey, e);
        }
    }

    private Path resolve(String key) {
        Path resolved = basePath.resolve(key).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return resolved;
    }
}
