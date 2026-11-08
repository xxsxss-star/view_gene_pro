package com.viewgene.application.task.execution;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import com.viewgene.infrastructure.persistence.record.TaskExecutionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionTaskExecutorTest {
    private ProductionTaskMapper mapper;
    private TaskStateService states;
    private MaterialCheckStep material;
    private SimulatedGenerationStep generation;
    private ResultFinalizationStep finalization;
    private ProductionTaskExecutor executor;

    @BeforeEach
    void setUp() {
        mapper = mock(ProductionTaskMapper.class);
        states = mock(TaskStateService.class);
        material = mock(MaterialCheckStep.class);
        generation = mock(SimulatedGenerationStep.class);
        finalization = mock(ResultFinalizationStep.class);
        executor = new ProductionTaskExecutor(mapper, states, material, generation, finalization);
        when(mapper.findExecutionById(7L)).thenReturn(Optional.of(new TaskExecutionRecord(7L, "/result.svg")));
        when(states.start(7L)).thenReturn(true);
        when(states.advance(7L, TaskStage.MATERIAL_CHECK, TaskStage.GENERATING)).thenReturn(true);
        when(states.advance(7L, TaskStage.GENERATING, TaskStage.FINALIZING)).thenReturn(true);
        when(states.succeed(7L, "/result.svg")).thenReturn(true);
    }

    @Test
    void executesThreeStepsInStrictOrderAndPersistsSuccess() {
        executor.execute(7L);

        InOrder order = inOrder(states, material, generation, finalization);
        order.verify(states).start(7L);
        order.verify(material).execute(new TaskExecutionContext(7L, "/result.svg"));
        order.verify(states).advance(7L, TaskStage.MATERIAL_CHECK, TaskStage.GENERATING);
        order.verify(generation).execute(new TaskExecutionContext(7L, "/result.svg"));
        order.verify(states).advance(7L, TaskStage.GENERATING, TaskStage.FINALIZING);
        order.verify(finalization).execute(new TaskExecutionContext(7L, "/result.svg"));
        order.verify(states).succeed(7L, "/result.svg");
    }

    @Test
    void stopsAfterMaterialFailure() {
        org.mockito.Mockito.doThrow(new TaskStepException("SIMULATED_MATERIAL_CHECK_FAILURE", "failed",
                TaskStage.MATERIAL_CHECK)).when(material).execute(org.mockito.ArgumentMatchers.any());
        executor.execute(7L);
        verify(states).failProcessing(7L, TaskStage.MATERIAL_CHECK, "SIMULATED_MATERIAL_CHECK_FAILURE", "failed");
        verify(generation, never()).execute(org.mockito.ArgumentMatchers.any());
        verify(finalization, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void stopsAfterGenerationFailure() {
        org.mockito.Mockito.doThrow(new TaskStepException("SIMULATED_GENERATION_FAILURE", "failed",
                TaskStage.GENERATING)).when(generation).execute(org.mockito.ArgumentMatchers.any());
        executor.execute(7L);
        verify(states).failProcessing(7L, TaskStage.GENERATING, "SIMULATED_GENERATION_FAILURE", "failed");
        verify(finalization, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsFinalizationFailureWithoutSuccess() {
        org.mockito.Mockito.doThrow(new TaskStepException("SIMULATED_FINALIZATION_FAILURE", "failed",
                TaskStage.FINALIZING)).when(finalization).execute(org.mockito.ArgumentMatchers.any());
        executor.execute(7L);
        verify(states).failProcessing(7L, TaskStage.FINALIZING, "SIMULATED_FINALIZATION_FAILURE", "failed");
        verify(states, never()).succeed(7L, "/result.svg");
    }

    @Test
    void staleOrTerminalTaskCannotStart() {
        when(states.start(7L)).thenReturn(false);
        executor.execute(7L);
        verify(material, never()).execute(org.mockito.ArgumentMatchers.any());
        verify(states, never()).succeed(7L, "/result.svg");
    }
}
