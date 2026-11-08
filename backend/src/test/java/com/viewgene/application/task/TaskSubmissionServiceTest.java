package com.viewgene.application.task;

import com.viewgene.application.task.execution.ProductionTaskExecutor;
import com.viewgene.application.task.execution.TaskStateService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TaskSubmissionServiceTest {
    @Test
    void queueRejectionMarksCommittedTaskFailedAndReturnsTaskId() {
        TaskExecutor rejecting = task -> { throw new TaskRejectedException("full"); };
        ProductionTaskExecutor executor = mock(ProductionTaskExecutor.class);
        TaskStateService states = mock(TaskStateService.class);
        TaskSubmissionService service = new TaskSubmissionService(rejecting, executor, states);

        assertThatThrownBy(() -> service.submit(77L))
                .isInstanceOf(TaskQueueFullException.class)
                .extracting(error -> ((TaskQueueFullException) error).getTaskId())
                .isEqualTo(77L);
        verify(states).failQueued(77L);
    }

    @Test
    void acceptedSubmissionOnlyPassesTaskIdToBackgroundExecutor() {
        TaskExecutor direct = Runnable::run;
        ProductionTaskExecutor executor = mock(ProductionTaskExecutor.class);
        TaskStateService states = mock(TaskStateService.class);
        new TaskSubmissionService(direct, executor, states).submit(78L);
        verify(executor).execute(78L);
    }
}
