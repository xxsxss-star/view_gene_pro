package com.viewgene.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String uploadDir) {
    public StorageProperties {
        if (uploadDir == null || uploadDir.isBlank()) {
            throw new IllegalArgumentException("app.storage.upload-dir must not be blank");
        }
    }
}
