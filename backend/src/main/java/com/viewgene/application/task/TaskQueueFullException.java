package com.viewgene.application.task;

public class TaskQueueFullException extends RuntimeException {
    private final long taskId;

    public TaskQueueFullException(long taskId) {
        super("任务执行队列已满");
        this.taskId = taskId;
    }

    public long getTaskId() { return taskId; }
}
