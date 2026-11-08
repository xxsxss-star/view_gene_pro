package com.viewgene.application.asset;

import java.time.LocalDateTime;

public record AssetDto(
        long id,
        String originalName,
        String mimeType,
        long fileSizeBytes,
        String contentUrl,
        LocalDateTime createdAt
) {
}
