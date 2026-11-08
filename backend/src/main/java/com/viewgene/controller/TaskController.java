package com.viewgene.controller;

import com.viewgene.application.task.CreateTaskRequest;
import com.viewgene.application.task.CreatedTaskDto;
import com.viewgene.application.task.TaskDto;
import com.viewgene.application.task.TaskService;
import com.viewgene.common.api.ApiResponse;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreatedTaskDto>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return ResponseEntity.accepted().body(ApiResponse.success(taskService.create(user, request)));
    }

    @GetMapping
    public ApiResponse<List<TaskDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(taskService.list(user, status));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskDto> detail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long taskId
    ) {
        return ApiResponse.success(taskService.detail(user, taskId));
    }
}
