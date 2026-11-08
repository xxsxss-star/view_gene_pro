package com.viewgene.application.task;

import com.viewgene.common.exception.BusinessException;
import com.viewgene.domain.model.Asset;
import com.viewgene.domain.model.AssetStatus;
import com.viewgene.domain.model.MaterialTemplate;
import com.viewgene.domain.model.TaskStatus;
import com.viewgene.infrastructure.persistence.mapper.AssetMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import com.viewgene.infrastructure.persistence.record.TaskAssetInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskViewRecord;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import com.viewgene.infrastructure.storage.AssetStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "demo_user", "演示用户");
    private MaterialTemplateMapper templateMapper;
    private AssetMapper assetMapper;
    private ProductionTaskMapper taskMapper;
    private AssetStorage storage;
    private TaskService service;
    private MaterialTemplate template;
    private Asset first;
    private Asset second;

    @BeforeEach
    void setUp() {
        templateMapper = mock(MaterialTemplateMapper.class);
        assetMapper = mock(AssetMapper.class);
        taskMapper = mock(ProductionTaskMapper.class);
        storage = mock(AssetStorage.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T10:30:00Z"), ZoneOffset.UTC);
        service = new TaskService(templateMapper, assetMapper, taskMapper, storage, clock);
        template = template(2);
        first = asset(101L, 1L, "first.jpg", "one.jpg", "image/jpeg", 100L, AssetStatus.READY);
        second = asset(102L, 1L, "second.png", "two.png", "image/png", 200L, AssetStatus.READY);
        when(templateMapper.findEnabledById("travel-collage")).thenReturn(Optional.of(template));
        when(assetMapper.findAllByIdsAndUserId(List.of(101L, 102L), 1L)).thenReturn(List.of(second, first));
        when(storage.load(any())).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(taskMapper.insert(any())).thenAnswer(invocation -> {
            TaskInsertRecord task = invocation.getArgument(0);
            task.setId(9001L);
            return 1;
        });
        when(taskMapper.insertAssets(eq(9001L), any())).thenReturn(2);
    }

    @Test
    void createsQueuedTaskAndPreservesRequestedAssetOrder() {
        CreatedTaskDto created = service.create(user,
                new CreateTaskRequest("travel-collage", List.of(101L, 102L)));

        assertThat(created.id()).isEqualTo(9001L);
        assertThat(created.status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(created.stage()).isNull();
        ArgumentCaptor<List<TaskAssetInsertRecord>> associations = ArgumentCaptor.forClass(List.class);
        verify(taskMapper).insertAssets(eq(9001L), associations.capture());
        assertThat(associations.getValue())
                .extracting(TaskAssetInsertRecord::position, TaskAssetInsertRecord::assetId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, 101L), org.assertj.core.groups.Tuple.tuple(2, 102L));
    }

    @Test
    void createMethodDefinesOneTransactionForTaskAndAssociations() throws Exception {
        Method create = TaskService.class.getMethod("create", AuthenticatedUser.class, CreateTaskRequest.class);
        assertThat(create.getAnnotation(Transactional.class)).isNotNull();

        when(taskMapper.insertAssets(eq(9001L), any())).thenReturn(0);
        assertThatThrownBy(() -> service.create(user,
                new CreateTaskRequest("travel-collage", List.of(101L, 102L))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWrongCountDuplicatesMissingCrossUserAndMissingPhysicalFile() {
        assertCode("INVALID_ASSET_COUNT", () -> service.create(user,
                new CreateTaskRequest("travel-collage", List.of(101L))));
        assertCode("VALIDATION_ERROR", () -> service.create(user,
                new CreateTaskRequest("travel-collage", List.of(101L, 101L))));

        when(assetMapper.findAllByIdsAndUserId(List.of(101L, 999L), 1L)).thenReturn(List.of(first));
        assertCode("ASSET_NOT_FOUND", () -> service.create(user,
                new CreateTaskRequest("travel-collage", List.of(101L, 999L))));

        when(storage.load("two.png")).thenReturn(new ByteArrayResource(new byte[0]) {
            @Override public boolean exists() { return false; }
        });
        assertCode("ASSET_NOT_FOUND", () -> service.create(user,
                new CreateTaskRequest("travel-collage", List.of(101L, 102L))));
        verify(taskMapper, never()).insertAssets(anyLong(), any());
    }

    @Test
    void queriesAlwaysIncludeCurrentUserAndDoNotWrite() {
        TaskViewRecord record = new TaskViewRecord(9001L, "travel-collage", "双图旅行拼贴", "/cover.svg",
                TaskStatus.QUEUED, null, null, null, null, LocalDateTime.now(), null, null, LocalDateTime.now());
        when(taskMapper.findAllByUserId(1L, TaskStatus.QUEUED)).thenReturn(List.of(record));
        when(taskMapper.findByIdAndUserId(9001L, 1L)).thenReturn(Optional.of(record));

        assertThat(service.list(user, "queued")).hasSize(1);
        assertThat(service.detail(user, 9001L).id()).isEqualTo(9001L);
        verify(taskMapper).findAllByUserId(1L, TaskStatus.QUEUED);
        verify(taskMapper).findByIdAndUserId(9001L, 1L);
        verify(taskMapper, never()).insert(any());
    }

    private MaterialTemplate template(int count) {
        LocalDateTime now = LocalDateTime.now();
        return new MaterialTemplate("travel-collage", 1L, "双图旅行拼贴", "说明", "/cover.svg", "/result.svg",
                count, List.of("image/jpeg", "image/png"), 1024L, true, 1, now, now);
    }

    private Asset asset(long id, long userId, String name, String storageKey, String mime, long size, AssetStatus status) {
        return new Asset(id, userId, name, storageKey, mime, size, "sha", status, LocalDateTime.now());
    }

    private void assertCode(String code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(code);
    }
}
