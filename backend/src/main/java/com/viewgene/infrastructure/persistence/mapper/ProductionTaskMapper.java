package com.viewgene.infrastructure.persistence.mapper;

import com.viewgene.domain.model.TaskStatus;
import com.viewgene.infrastructure.persistence.record.TaskInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskAssetInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskViewRecord;
import com.viewgene.infrastructure.persistence.record.TaskExecutionRecord;
import com.viewgene.domain.model.TaskStage;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface ProductionTaskMapper {
    int insert(TaskInsertRecord task);

    int insertAssets(@Param("taskId") long taskId, @Param("assets") List<TaskAssetInsertRecord> assets);

    List<TaskViewRecord> findAllByUserId(@Param("userId") long userId, @Param("status") TaskStatus status);

    Optional<TaskViewRecord> findByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

    Optional<TaskExecutionRecord> findExecutionById(@Param("id") long id);

    int start(@Param("id") long id, @Param("stage") TaskStage stage, @Param("now") LocalDateTime now);

    int advance(@Param("id") long id, @Param("expectedStage") TaskStage expectedStage,
                @Param("nextStage") TaskStage nextStage, @Param("now") LocalDateTime now);

    int succeed(@Param("id") long id, @Param("expectedStage") TaskStage expectedStage,
                @Param("resultUrl") String resultUrl, @Param("now") LocalDateTime now);

    int failProcessing(@Param("id") long id, @Param("stage") TaskStage stage,
                       @Param("code") String code, @Param("reason") String reason,
                       @Param("now") LocalDateTime now);

    int failQueued(@Param("id") long id, @Param("code") String code,
                   @Param("reason") String reason, @Param("now") LocalDateTime now);
}
