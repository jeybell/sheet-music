package com.jeybell.sheetmusic.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String type,
        Local local,
        R2 r2
) {

    public record Local(String basePath) {}

    public record R2(String endpoint, String accessKey, String secretKey, String bucket) {}
}
