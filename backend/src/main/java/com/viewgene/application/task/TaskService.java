package com.viewgene.application.task;

import com.viewgene.common.exception.BusinessException;
import com.viewgene.common.exception.ResourceNotFoundException;
import com.viewgene.domain.model.TaskStatus;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import com.viewgene.infrastructure.persistence.record.TaskViewRecord;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {
    private final TaskCreationService creationService;
    private final TaskSubmissionService submissionService;
    private final ProductionTaskMapper taskMapper;

    public TaskService(TaskCreationService creationService, TaskSubmissionService submissionService,
                       ProductionTaskMapper taskMapper) {
        this.creationService = creationService;
        this.submissionService = submissionService;
        this.taskMapper = taskMapper;
    }

    public CreatedTaskDto create(AuthenticatedUser user, CreateTaskRequest request) {
        // Spring 代理返回时创建事务已提交，此后异步线程才能安全地按 taskId 重新读取任务。
        CreatedTaskDto created = creationService.createCommitted(user, request);
        submissionService.submit(created.id());
        return created;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> list(AuthenticatedUser user, String statusValue) {
        TaskStatus status = parseStatus(statusValue);
        return taskMapper.findAllByUserId(user.id(), status).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TaskDto detail(AuthenticatedUser user, long taskId) {
        return taskMapper.findByIdAndUserId(taskId, user.id())
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("TASK_NOT_FOUND", "任务不存在"));
    }

    private TaskStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("VALIDATION_ERROR", "任务状态无效", HttpStatus.BAD_REQUEST);
        }
    }

    private TaskDto toDto(TaskViewRecord task) {
        TaskFailureDto failure = task.failureCode() == null ? null
                : new TaskFailureDto(task.failureCode(), task.failureReason(), task.stage());
        return new TaskDto(task.id(),
                new TaskTemplateDto(task.templateId(), task.templateTitle(), task.templateCoverUrl()),
                task.status(), task.stage(), task.resultUrl(), failure, task.createdAt(), task.startedAt(),
                task.completedAt(), task.updatedAt());
    }
}
