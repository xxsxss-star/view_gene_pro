package com.viewgene.domain.model;

public record TemplateCategory(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        Boolean enabled
) {
}
