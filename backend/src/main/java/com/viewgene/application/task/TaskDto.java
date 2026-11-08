package com.viewgene.application.task;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.domain.model.TaskStatus;

import java.time.LocalDateTime;

public record TaskDto(
        long id,
        TaskTemplateDto template,
        TaskStatus status,
        TaskStage stage,
        String resultUrl,
        TaskFailureDto failure,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {
}
