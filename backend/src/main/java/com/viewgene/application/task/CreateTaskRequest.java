package com.viewgene.application.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateTaskRequest(
        @NotBlank String templateId,
        @NotEmpty List<Long> assetIds
) {
}
