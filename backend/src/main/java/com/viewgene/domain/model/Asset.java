package com.viewgene.domain.model;

import java.time.LocalDateTime;

public record Asset(
        Long id,
        Long userId,
        String originalName,
        String storageKey,
        String mimeType,
        Long fileSizeBytes,
        String sha256,
        AssetStatus status,
        LocalDateTime createdAt
) {
}
