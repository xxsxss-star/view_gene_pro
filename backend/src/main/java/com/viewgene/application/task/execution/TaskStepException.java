package com.viewgene.application.task.execution;

import com.viewgene.domain.model.TaskStage;

public class TaskStepException extends RuntimeException {
    private final String code;
    private final TaskStage stage;

    public TaskStepException(String code, String message, TaskStage stage) {
        super(message);
        this.code = code;
        this.stage = stage;
    }

    public String getCode() { return code; }

    public TaskStage getStage() { return stage; }
}
