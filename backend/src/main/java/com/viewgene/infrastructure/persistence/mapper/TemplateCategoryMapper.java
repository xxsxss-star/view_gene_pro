package com.viewgene.infrastructure.persistence.mapper;

import com.viewgene.domain.model.TemplateCategory;

import java.util.List;

public interface TemplateCategoryMapper {
    List<TemplateCategory> findAllEnabled();
}
