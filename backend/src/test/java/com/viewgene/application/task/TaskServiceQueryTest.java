package com.viewgene.application.task;

import com.viewgene.domain.model.TaskStatus;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import com.viewgene.infrastructure.persistence.record.TaskViewRecord;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceQueryTest {
    @Test
    void createsThenSubmitsOnlyAfterCreationServiceReturns() {
        TaskCreationService creation = mock(TaskCreationService.class);
        TaskSubmissionService submission = mock(TaskSubmissionService.class);
        ProductionTaskMapper mapper = mock(ProductionTaskMapper.class);
        AuthenticatedUser user = new AuthenticatedUser(1L, "demo", "Demo");
        CreateTaskRequest request = new CreateTaskRequest("template", List.of(1L));
        CreatedTaskDto created = new CreatedTaskDto(9L, TaskStatus.QUEUED, null, LocalDateTime.now());
        when(creation.createCommitted(user, request)).thenReturn(created);

        assertThat(new TaskService(creation, submission, mapper).create(user, request)).isEqualTo(created);
        verify(submission).submit(9L);
    }

    @Test
    void queriesAlwaysIncludeCurrentUserAndDoNotAdvanceTask() {
        TaskCreationService creation = mock(TaskCreationService.class);
        TaskSubmissionService submission = mock(TaskSubmissionService.class);
        ProductionTaskMapper mapper = mock(ProductionTaskMapper.class);
        TaskService service = new TaskService(creation, submission, mapper);
        AuthenticatedUser user = new AuthenticatedUser(1L, "demo", "Demo");
        TaskViewRecord record = new TaskViewRecord(9L, "template", "模板", "/cover.svg",
                TaskStatus.PROCESSING, com.viewgene.domain.model.TaskStage.GENERATING, null,
                null, null, LocalDateTime.now(), LocalDateTime.now(), null, LocalDateTime.now());
        when(mapper.findAllByUserId(1L, TaskStatus.PROCESSING)).thenReturn(List.of(record));
        when(mapper.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(record));

        assertThat(service.list(user, "processing")).hasSize(1);
        assertThat(service.detail(user, 9L).stage()).isEqualTo(com.viewgene.domain.model.TaskStage.GENERATING);
        verify(mapper).findAllByUserId(1L, TaskStatus.PROCESSING);
        verify(mapper).findByIdAndUserId(9L, 1L);
    }
}
