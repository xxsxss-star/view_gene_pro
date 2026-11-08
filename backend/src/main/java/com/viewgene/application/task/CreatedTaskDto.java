package com.viewgene.application.task;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.domain.model.TaskStatus;

import java.time.LocalDateTime;

public record CreatedTaskDto(long id, TaskStatus status, TaskStage stage, LocalDateTime createdAt) {
}
