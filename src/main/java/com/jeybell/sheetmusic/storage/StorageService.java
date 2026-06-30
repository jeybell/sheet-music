package com.jeybell.sheetmusic.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface StorageService {

    void store(String key, InputStream inputStream, long size, String contentType);

    Resource load(String key);

    void delete(String key);

    void copy(String sourceKey, String destKey);
}
