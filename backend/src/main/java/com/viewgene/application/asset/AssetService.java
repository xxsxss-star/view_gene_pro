package com.viewgene.application.asset;

import com.viewgene.common.exception.BusinessException;
import com.viewgene.common.exception.ResourceNotFoundException;
import com.viewgene.domain.model.Asset;
import com.viewgene.domain.model.AssetStatus;
import com.viewgene.domain.model.MaterialTemplate;
import com.viewgene.infrastructure.persistence.mapper.AssetMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.record.AssetInsertRecord;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import com.viewgene.infrastructure.storage.AssetStorage;
import com.viewgene.infrastructure.storage.DetectedImageType;
import com.viewgene.infrastructure.storage.ImageSignatureDetector;
import com.viewgene.infrastructure.storage.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AssetService {
    private final MaterialTemplateMapper templateMapper;
    private final AssetMapper assetMapper;
    private final AssetStorage storage;
    private final Clock clock;

    @Autowired
    public AssetService(MaterialTemplateMapper templateMapper, AssetMapper assetMapper, AssetStorage storage) {
        this(templateMapper, assetMapper, storage, Clock.systemUTC());
    }

    AssetService(MaterialTemplateMapper templateMapper, AssetMapper assetMapper, AssetStorage storage, Clock clock) {
        this.templateMapper = templateMapper;
        this.assetMapper = assetMapper;
        this.storage = storage;
        this.clock = clock;
    }

    public AssetDto upload(AuthenticatedUser user, String templateId, MultipartFile file) {
        MaterialTemplate template = templateMapper.findEnabledById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TEMPLATE_NOT_FOUND", "模板不存在"));
        validateNotEmpty(file);
        if (file.getSize() > template.maxFileSizeBytes()) {
            throw new BusinessException("FILE_TOO_LARGE", "图片超过模板允许的大小", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        DetectedImageType imageType = detectImageType(file);
        if (!template.acceptedMimeTypes().contains(imageType.mimeType())) {
            throw new BusinessException("UNSUPPORTED_MEDIA_TYPE", "模板不支持该图片类型", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        StoredFile stored = store(file, imageType);
        if (stored.size() <= 0 || stored.size() > template.maxFileSizeBytes()) {
            storage.delete(stored.storageKey());
            throw new BusinessException("FILE_TOO_LARGE", "图片超过模板允许的大小", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        LocalDateTime createdAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        AssetInsertRecord record = new AssetInsertRecord(
                user.id(), normalizeOriginalName(file.getOriginalFilename(), imageType), stored.storageKey(),
                imageType.mimeType(), stored.size(), stored.sha256(), AssetStatus.READY, createdAt
        );
        try {
            int inserted = assetMapper.insert(record);
            if (inserted != 1 || record.getId() == null) {
                throw new IllegalStateException("Asset metadata insert did not return an id");
            }
        } catch (RuntimeException exception) {
            // 文件先落盘、元数据后入库；数据库失败时必须补偿删除，避免产生孤儿文件。
            storage.delete(stored.storageKey());
            throw exception;
        }
        return new AssetDto(record.getId(), record.getOriginalName(), record.getMimeType(), record.getFileSizeBytes(),
                "/api/v1/assets/" + record.getId() + "/content", createdAt);
    }

    public AssetContent content(AuthenticatedUser user, long assetId) {
        Asset asset = assetMapper.findByIdAndUserId(assetId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException("ASSET_NOT_FOUND", "素材不存在"));
        Resource resource;
        try {
            resource = storage.load(asset.storageKey());
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException("ASSET_NOT_FOUND", "素材不存在");
        }
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("ASSET_NOT_FOUND", "素材不存在");
        }
        return new AssetContent(resource, asset.originalName(), asset.mimeType(), asset.fileSizeBytes());
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "请选择非空图片", HttpStatus.BAD_REQUEST);
        }
    }

    private DetectedImageType detectImageType(MultipartFile file) {
        try {
            return ImageSignatureDetector.detect(file)
                    .orElseThrow(() -> new BusinessException(
                            "UNSUPPORTED_MEDIA_TYPE", "文件内容不是支持的 JPG 或 PNG 图片", HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        } catch (IOException exception) {
            throw new BusinessException("FILE_STORAGE_ERROR", "图片读取失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private StoredFile store(MultipartFile file, DetectedImageType imageType) {
        try {
            return storage.store(file, imageType);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) throw businessException;
            throw new BusinessException("FILE_STORAGE_ERROR", "图片保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String normalizeOriginalName(String originalName, DetectedImageType imageType) {
        String normalized = originalName == null ? "" : originalName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_").trim();
        if (normalized.isBlank()) normalized = "upload" + imageType.extension();
        return normalized.length() <= 255 ? normalized : normalized.substring(normalized.length() - 255);
    }
}
