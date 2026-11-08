package com.viewgene.application.task.execution;

import com.viewgene.domain.model.TaskStage;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskStateServiceTest {
    @Test
    void everyStateChangeUsesAnIndependentShortTransaction() {
        for (String name : new String[]{"start", "advance", "succeed", "failProcessing", "failQueued"}) {
            Method method = java.util.Arrays.stream(TaskStateService.class.getMethods())
                    .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional).isNotNull();
            assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        }
    }

    @Test
    void zeroUpdatedRowsSignalsTerminalOrStaleState() {
        ProductionTaskMapper mapper = mock(ProductionTaskMapper.class);
        when(mapper.advance(org.mockito.ArgumentMatchers.eq(1L), any(), any(), any())).thenReturn(0);
        TaskStateService service = new TaskStateService(mapper, java.time.Clock.systemUTC());
        assertThat(service.advance(1L, TaskStage.MATERIAL_CHECK, TaskStage.GENERATING)).isFalse();
        verify(mapper).advance(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(TaskStage.MATERIAL_CHECK),
                org.mockito.ArgumentMatchers.eq(TaskStage.GENERATING), any());
    }
}
