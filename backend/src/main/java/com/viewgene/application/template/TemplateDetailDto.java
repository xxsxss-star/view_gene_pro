package com.viewgene.application.template;

import java.util.List;

public record TemplateDetailDto(
        String id,
        String title,
        String categoryCode,
        String categoryName,
        String coverUrl,
        String description,
        int requiredImageCount,
        List<String> acceptedMimeTypes,
        long maxFileSizeBytes
) {
}
