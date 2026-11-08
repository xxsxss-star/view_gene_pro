package com.viewgene.application.task.execution;

import com.viewgene.config.SimulationProperties;
import com.viewgene.domain.model.TaskStage;
import org.springframework.stereotype.Component;

@Component
public class MaterialCheckStep extends AbstractSimulationStep {
    public MaterialCheckStep(SimulationProperties properties) {
        super(properties.materialCheckDelayMs(), properties.failureStage());
    }

    @Override public TaskStage stage() { return TaskStage.MATERIAL_CHECK; }
    @Override protected String failureCode() { return "SIMULATED_MATERIAL_CHECK_FAILURE"; }
    @Override protected String failureMessage() { return "已按模拟配置触发素材检查失败"; }
}
