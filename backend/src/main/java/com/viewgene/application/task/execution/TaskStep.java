package com.viewgene.application.task.execution;

import com.viewgene.domain.model.TaskStage;

public interface TaskStep {
    TaskStage stage();

    void execute(TaskExecutionContext context);
}
