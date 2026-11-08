package com.viewgene.common.api;

import java.util.List;

public record ApiError(String code, String message, List<FieldErrorDetail> fieldErrors) {
    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
