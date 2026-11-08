package com.viewgene.common.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viewgene.common.web.RequestIdContext;

import java.time.Instant;
import java.util.List;

public record TaskQueueFullErrorResponse(
        ApiError error,
        long taskId,
        String requestId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {
    public static TaskQueueFullErrorResponse of(long taskId, String message) {
        return new TaskQueueFullErrorResponse(
                new ApiError("TASK_QUEUE_FULL", message, List.of()), taskId,
                RequestIdContext.currentOrCreate(), Instant.now());
    }
}
