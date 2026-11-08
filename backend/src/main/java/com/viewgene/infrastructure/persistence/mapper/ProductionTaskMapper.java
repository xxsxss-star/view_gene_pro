package com.viewgene.infrastructure.persistence.mapper;

import com.viewgene.domain.model.TaskStatus;
import com.viewgene.infrastructure.persistence.record.TaskInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskAssetInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskViewRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface ProductionTaskMapper {
    int insert(TaskInsertRecord task);

    int insertAssets(@Param("taskId") long taskId, @Param("assets") List<TaskAssetInsertRecord> assets);

    List<TaskViewRecord> findAllByUserId(@Param("userId") long userId, @Param("status") TaskStatus status);

    Optional<TaskViewRecord> findByIdAndUserId(@Param("id") long id, @Param("userId") long userId);
}
