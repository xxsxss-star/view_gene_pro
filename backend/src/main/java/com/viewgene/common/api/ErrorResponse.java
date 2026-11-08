package com.viewgene.common.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viewgene.common.web.RequestIdContext;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        ApiError error,
        String requestId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return of(code, message, List.of());
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(
                new ApiError(code, message, fieldErrors),
                RequestIdContext.currentOrCreate(),
                Instant.now()
        );
    }
}
