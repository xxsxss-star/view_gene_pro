package com.viewgene.domain.model;

import java.time.LocalDateTime;

public record ProductionTask(
        Long id,
        Long userId,
        String templateId,
        TaskStatus status,
        TaskStage stage,
        String resultUrl,
        String failureCode,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt,
        Integer version
) {
}
