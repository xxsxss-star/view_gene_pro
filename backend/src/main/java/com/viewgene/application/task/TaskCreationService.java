package com.viewgene.application.task;

import com.viewgene.common.exception.BusinessException;
import com.viewgene.common.exception.ResourceNotFoundException;
import com.viewgene.domain.model.Asset;
import com.viewgene.domain.model.AssetStatus;
import com.viewgene.domain.model.MaterialTemplate;
import com.viewgene.domain.model.TaskStatus;
import com.viewgene.infrastructure.persistence.mapper.AssetMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.mapper.ProductionTaskMapper;
import com.viewgene.infrastructure.persistence.record.TaskAssetInsertRecord;
import com.viewgene.infrastructure.persistence.record.TaskInsertRecord;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import com.viewgene.infrastructure.storage.AssetStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class TaskCreationService {
    private final MaterialTemplateMapper templateMapper;
    private final AssetMapper assetMapper;
    private final ProductionTaskMapper taskMapper;
    private final AssetStorage storage;
    private final Clock clock;

    @Autowired
    public TaskCreationService(MaterialTemplateMapper templateMapper, AssetMapper assetMapper,
                               ProductionTaskMapper taskMapper, AssetStorage storage) {
        this(templateMapper, assetMapper, taskMapper, storage, Clock.systemUTC());
    }

    TaskCreationService(MaterialTemplateMapper templateMapper, AssetMapper assetMapper,
                        ProductionTaskMapper taskMapper, AssetStorage storage, Clock clock) {
        this.templateMapper = templateMapper;
        this.assetMapper = assetMapper;
        this.taskMapper = taskMapper;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public CreatedTaskDto createCommitted(AuthenticatedUser user, CreateTaskRequest request) {
        MaterialTemplate template = templateMapper.findEnabledById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("TEMPLATE_NOT_FOUND", "模板不存在"));
        List<Long> assetIds = request.assetIds();
        if (assetIds.size() != template.requiredImageCount()) {
            throw new BusinessException("INVALID_ASSET_COUNT",
                    template.title() + "要求上传" + template.requiredImageCount() + "张图片", HttpStatus.BAD_REQUEST);
        }
        if (assetIds.stream().anyMatch(id -> id == null) || new HashSet<>(assetIds).size() != assetIds.size()) {
            throw new BusinessException("VALIDATION_ERROR", "素材 ID 不能为空或重复", HttpStatus.BAD_REQUEST);
        }

        List<Asset> assets = assetMapper.findAllByIdsAndUserId(assetIds, user.id());
        if (assets.size() != assetIds.size()) {
            throw new ResourceNotFoundException("ASSET_NOT_FOUND", "素材不存在");
        }
        Map<Long, Asset> assetsById = new HashMap<>();
        assets.forEach(asset -> assetsById.put(asset.id(), asset));
        for (Long assetId : assetIds) validateAsset(assetsById.get(assetId), template);

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        TaskInsertRecord task = new TaskInsertRecord(user.id(), template.id(), TaskStatus.QUEUED, now, now);
        if (taskMapper.insert(task) != 1 || task.getId() == null) {
            throw new IllegalStateException("Task insert did not return an id");
        }
        List<TaskAssetInsertRecord> associations = java.util.stream.IntStream.range(0, assetIds.size())
                .mapToObj(index -> new TaskAssetInsertRecord(index + 1, assetIds.get(index)))
                .toList();
        if (taskMapper.insertAssets(task.getId(), associations) != assetIds.size()) {
            throw new IllegalStateException("Task asset association count mismatch");
        }
        return new CreatedTaskDto(task.getId(), TaskStatus.QUEUED, null, now);
    }

    private void validateAsset(Asset asset, MaterialTemplate template) {
        if (asset == null) throw new ResourceNotFoundException("ASSET_NOT_FOUND", "素材不存在");
        if (asset.status() != AssetStatus.READY) {
            throw new BusinessException("VALIDATION_ERROR", "素材尚未就绪", HttpStatus.BAD_REQUEST);
        }
        if (!template.acceptedMimeTypes().contains(asset.mimeType())) {
            throw new BusinessException("UNSUPPORTED_MEDIA_TYPE", "素材类型不符合模板要求", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        if (asset.fileSizeBytes() <= 0 || asset.fileSizeBytes() > template.maxFileSizeBytes()) {
            throw new BusinessException("FILE_TOO_LARGE", "素材大小不符合模板要求", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        try {
            if (!storage.load(asset.storageKey()).exists()) {
                throw new ResourceNotFoundException("ASSET_NOT_FOUND", "素材文件不存在");
            }
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException("ASSET_NOT_FOUND", "素材文件不存在");
        }
    }
}
