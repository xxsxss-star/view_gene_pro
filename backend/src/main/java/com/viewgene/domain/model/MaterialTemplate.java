package com.viewgene.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record MaterialTemplate(
        String id,
        Long categoryId,
        String title,
        String description,
        String coverUrl,
        String resultUrl,
        Integer requiredImageCount,
        List<String> acceptedMimeTypes,
        Long maxFileSizeBytes,
        Boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
