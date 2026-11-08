package com.viewgene.application.task;

import com.viewgene.application.task.execution.ProductionTaskExecutor;
import com.viewgene.application.task.execution.TaskStateService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class TaskSubmissionService {
    private final TaskExecutor taskExecutor;
    private final ProductionTaskExecutor productionTaskExecutor;
    private final TaskStateService states;

    public TaskSubmissionService(@Qualifier("productionTaskPool") TaskExecutor taskExecutor,
                                 ProductionTaskExecutor productionTaskExecutor, TaskStateService states) {
        this.taskExecutor = taskExecutor;
        this.productionTaskExecutor = productionTaskExecutor;
        this.states = states;
    }

    public void submit(long taskId) {
        try {
            taskExecutor.execute(() -> productionTaskExecutor.execute(taskId));
        } catch (TaskRejectedException exception) {
            // 创建事务已经提交，拒绝时必须做补偿更新，不能留下永久 QUEUED。
            states.failQueued(taskId);
            throw new TaskQueueFullException(taskId);
        }
    }
}
