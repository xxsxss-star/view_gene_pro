package com.viewgene.application.template;

public record TemplateCardDto(
        String id,
        String title,
        String categoryCode,
        String categoryName,
        String coverUrl,
        String description,
        int requiredImageCount
) {
}
