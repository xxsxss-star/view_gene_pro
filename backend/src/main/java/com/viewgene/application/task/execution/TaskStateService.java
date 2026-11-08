package com.viewgene.application.task.execution;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TaskStateService {
    private final ProductionTaskMapper mapper;
    private final Clock clock;

    @Autowired
    public TaskStateService(ProductionTaskMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    TaskStateService(ProductionTaskMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean start(long taskId) {
        return mapper.start(taskId, TaskStage.MATERIAL_CHECK, now()) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean advance(long taskId, TaskStage expected, TaskStage next) {
        return mapper.advance(taskId, expected, next, now()) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean succeed(long taskId, String resultUrl) {
        return mapper.succeed(taskId, TaskStage.FINALIZING, resultUrl, now()) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean failProcessing(long taskId, TaskStage stage, String code, String reason) {
        return mapper.failProcessing(taskId, stage, code, reason, now()) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean failQueued(long taskId) {
        return mapper.failQueued(taskId, "TASK_QUEUE_FULL", "任务执行队列已满", now()) == 1;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
