package com.viewgene.infrastructure.persistence.record;

import com.viewgene.domain.model.TaskStatus;

import java.time.LocalDateTime;

/** MyBatis 写入任务时使用的主键承载对象。 */
public class TaskInsertRecord {
    private Long id;
    private final long userId;
    private final String templateId;
    private final TaskStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public TaskInsertRecord(long userId, String templateId, TaskStatus status,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.templateId = templateId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getUserId() { return userId; }
    public String getTemplateId() { return templateId; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
