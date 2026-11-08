package com.viewgene.application.task.execution;

import com.viewgene.config.SimulationProperties;
import com.viewgene.domain.model.TaskStage;
import org.springframework.stereotype.Component;

@Component
public class ResultFinalizationStep extends AbstractSimulationStep {
    public ResultFinalizationStep(SimulationProperties properties) {
        super(properties.finalizingDelayMs(), properties.failureStage());
    }

    @Override public TaskStage stage() { return TaskStage.FINALIZING; }
    @Override protected String failureCode() { return "SIMULATED_FINALIZATION_FAILURE"; }
    @Override protected String failureMessage() { return "已按模拟配置触发结果整理失败"; }
}
