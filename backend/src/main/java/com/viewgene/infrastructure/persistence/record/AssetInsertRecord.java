package com.viewgene.infrastructure.persistence.record;

import com.viewgene.domain.model.AssetStatus;

import java.time.LocalDateTime;

/** MyBatis 使用可变 id 接收数据库生成的主键，其余字段在构造后保持只读。 */
public class AssetInsertRecord {
    private Long id;
    private final long userId;
    private final String originalName;
    private final String storageKey;
    private final String mimeType;
    private final long fileSizeBytes;
    private final String sha256;
    private final AssetStatus status;
    private final LocalDateTime createdAt;

    public AssetInsertRecord(long userId, String originalName, String storageKey, String mimeType,
                             long fileSizeBytes, String sha256, AssetStatus status, LocalDateTime createdAt) {
        this.userId = userId;
        this.originalName = originalName;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256 = sha256;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getUserId() { return userId; }
    public String getOriginalName() { return originalName; }
    public String getStorageKey() { return storageKey; }
    public String getMimeType() { return mimeType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getSha256() { return sha256; }
    public AssetStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
