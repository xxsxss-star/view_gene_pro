package com.viewgene.application.template;

import com.viewgene.common.exception.ResourceNotFoundException;
import com.viewgene.domain.model.MaterialTemplate;
import com.viewgene.domain.model.TemplateCategory;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.mapper.TemplateCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateQueryService {
    private final TemplateCategoryMapper categoryMapper;
    private final MaterialTemplateMapper templateMapper;

    public TemplateQueryService(TemplateCategoryMapper categoryMapper, MaterialTemplateMapper templateMapper) {
        this.categoryMapper = categoryMapper;
        this.templateMapper = templateMapper;
    }

    public TemplateListDto list(String categoryCode) {
        List<TemplateCategory> categories = categoryMapper.findAllEnabled();
        Map<Long, TemplateCategory> categoriesById = indexCategories(categories);
        List<MaterialTemplate> templates = categoryCode == null || categoryCode.isBlank()
                ? templateMapper.findAllEnabled()
                : templateMapper.findAllEnabledByCategoryCode(categoryCode.trim());
        List<TemplateCardDto> items = templates.stream()
                .map(template -> toCard(template, categoriesById))
                .toList();
        List<TemplateCategoryDto> categoryDtos = categories.stream()
                .map(category -> new TemplateCategoryDto(category.code(), category.name()))
                .toList();
        return new TemplateListDto(categoryDtos, items, items.size());
    }

    public TemplateDetailDto detail(String templateId) {
        MaterialTemplate template = templateMapper.findEnabledById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TEMPLATE_NOT_FOUND", "模板不存在"));
        TemplateCategory category = categoryMapper.findAllEnabled().stream()
                .filter(candidate -> candidate.id().equals(template.categoryId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("TEMPLATE_NOT_FOUND", "模板不存在"));
        return new TemplateDetailDto(
                template.id(), template.title(), category.code(), category.name(), template.coverUrl(),
                template.description(), template.requiredImageCount(), List.copyOf(template.acceptedMimeTypes()),
                template.maxFileSizeBytes()
        );
    }

    private Map<Long, TemplateCategory> indexCategories(List<TemplateCategory> categories) {
        Map<Long, TemplateCategory> indexed = new LinkedHashMap<>();
        categories.forEach(category -> indexed.put(category.id(), category));
        return indexed;
    }

    private TemplateCardDto toCard(MaterialTemplate template, Map<Long, TemplateCategory> categories) {
        TemplateCategory category = categories.get(template.categoryId());
        if (category == null) {
            throw new IllegalStateException("Enabled template references a missing enabled category");
        }
        return new TemplateCardDto(
                template.id(), template.title(), category.code(), category.name(), template.coverUrl(),
                template.description(), template.requiredImageCount()
        );
    }
}
