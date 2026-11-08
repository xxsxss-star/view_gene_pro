package com.viewgene.application.task.execution;

import com.viewgene.domain.model.FailureStage;
import com.viewgene.domain.model.TaskStage;

abstract class AbstractSimulationStep implements TaskStep {
    private final long delayMs;
    private final FailureStage configuredFailure;

    protected AbstractSimulationStep(long delayMs, FailureStage configuredFailure) {
        this.delayMs = delayMs;
        this.configuredFailure = configuredFailure;
    }

    @Override
    public final void execute(TaskExecutionContext context) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TaskStepException("TASK_EXECUTION_INTERRUPTED", "任务处理被中断", stage());
        }
        if (configuredFailure.name().equals(stage().name())) {
            throw new TaskStepException(failureCode(), failureMessage(), stage());
        }
        perform(context);
    }

    protected void perform(TaskExecutionContext context) {
        // 模拟步骤默认只负责可配置耗时；子类可以补充阶段特有逻辑。
    }

    protected abstract String failureCode();

    protected abstract String failureMessage();
}
