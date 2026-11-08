package com.viewgene.infrastructure.persistence.mapper;

import com.viewgene.domain.model.MaterialTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialTemplateMapper {
    Optional<MaterialTemplate> findEnabledById(@Param("id") String id);

    List<MaterialTemplate> findAllEnabled();

    List<MaterialTemplate> findAllEnabledByCategoryCode(@Param("categoryCode") String categoryCode);
}
