package com.viewgene.application.task.execution;

import com.viewgene.config.SimulationProperties;
import com.viewgene.domain.model.TaskStage;
import org.springframework.stereotype.Component;

@Component
public class SimulatedGenerationStep extends AbstractSimulationStep {
    public SimulatedGenerationStep(SimulationProperties properties) {
        super(properties.generatingDelayMs(), properties.failureStage());
    }

    @Override public TaskStage stage() { return TaskStage.GENERATING; }
    @Override protected String failureCode() { return "SIMULATED_GENERATION_FAILURE"; }
    @Override protected String failureMessage() { return "已按模拟配置触发生成失败"; }
}
