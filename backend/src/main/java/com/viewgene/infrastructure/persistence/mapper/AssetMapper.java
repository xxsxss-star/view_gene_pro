package com.viewgene.infrastructure.persistence.mapper;

import com.viewgene.domain.model.Asset;
import com.viewgene.infrastructure.persistence.record.AssetInsertRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface AssetMapper {
    int insert(AssetInsertRecord asset);

    Optional<Asset> findByIdAndUserId(@Param("id") long id, @Param("userId") long userId);
}
