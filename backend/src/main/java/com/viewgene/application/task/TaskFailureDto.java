package com.viewgene.application.task;

import com.viewgene.domain.model.TaskStage;

public record TaskFailureDto(String code, String message, TaskStage stage) {
}
