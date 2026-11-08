package com.viewgene.common.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viewgene.common.web.RequestIdContext;

import java.time.Instant;

public record ApiResponse<T>(T data, String requestId, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, RequestIdContext.currentOrCreate(), Instant.now());
    }
}
