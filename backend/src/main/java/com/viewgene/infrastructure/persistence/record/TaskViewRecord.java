package com.viewgene.infrastructure.persistence.record;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.domain.model.TaskStatus;

import java.time.LocalDateTime;

public record TaskViewRecord(
        Long id,
        String templateId,
        String templateTitle,
        String templateCoverUrl,
        TaskStatus status,
        TaskStage stage,
        String resultUrl,
        String failureCode,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {
}
