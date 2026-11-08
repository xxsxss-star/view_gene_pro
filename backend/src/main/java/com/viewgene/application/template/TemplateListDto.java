package com.viewgene.application.template;

import java.util.List;

public record TemplateListDto(
        List<TemplateCategoryDto> categories,
        List<TemplateCardDto> items,
        int total
) {
}
