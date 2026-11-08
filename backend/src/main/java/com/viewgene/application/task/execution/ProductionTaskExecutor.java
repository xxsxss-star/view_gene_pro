package com.viewgene.application.task.execution;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import com.viewgene.infrastructure.persistence.record.TaskExecutionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductionTaskExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionTaskExecutor.class);
    private final ProductionTaskMapper mapper;
    private final TaskStateService states;
    private final MaterialCheckStep materialCheck;
    private final SimulatedGenerationStep generation;
    private final ResultFinalizationStep finalization;

    public ProductionTaskExecutor(ProductionTaskMapper mapper, TaskStateService states,
                                  MaterialCheckStep materialCheck, SimulatedGenerationStep generation,
                                  ResultFinalizationStep finalization) {
        this.mapper = mapper;
        this.states = states;
        this.materialCheck = materialCheck;
        this.generation = generation;
        this.finalization = finalization;
    }

    public void execute(long taskId) {
        TaskStage currentStage = TaskStage.MATERIAL_CHECK;
        try {
            TaskExecutionRecord task = mapper.findExecutionById(taskId).orElse(null);
            if (task == null || !states.start(taskId)) {
                LOGGER.warn("Task execution skipped, taskId={}, reason=not-queued-or-missing", taskId);
                return;
            }
            TaskExecutionContext context = new TaskExecutionContext(taskId, task.resultUrl());
            LOGGER.info("Task stage started, taskId={}, stage={}", taskId, currentStage);
            materialCheck.execute(context);

            if (!states.advance(taskId, TaskStage.MATERIAL_CHECK, TaskStage.GENERATING)) return;
            currentStage = TaskStage.GENERATING;
            LOGGER.info("Task stage started, taskId={}, stage={}", taskId, currentStage);
            generation.execute(context);

            if (!states.advance(taskId, TaskStage.GENERATING, TaskStage.FINALIZING)) return;
            currentStage = TaskStage.FINALIZING;
            LOGGER.info("Task stage started, taskId={}, stage={}", taskId, currentStage);
            finalization.execute(context);

            if (states.succeed(taskId, task.resultUrl())) {
                LOGGER.info("Task execution succeeded, taskId={}", taskId);
            }
        } catch (TaskStepException exception) {
            states.failProcessing(taskId, exception.getStage(), exception.getCode(), exception.getMessage());
            LOGGER.warn("Task execution failed, taskId={}, stage={}, code={}",
                    taskId, exception.getStage(), exception.getCode());
        } catch (RuntimeException exception) {
            states.failProcessing(taskId, currentStage, "TASK_EXECUTION_ERROR", "任务处理发生内部错误");
            LOGGER.error("Unexpected task execution failure, taskId={}, stage={}", taskId, currentStage, exception);
        }
    }
}
