package com.viewgene.config;

import com.viewgene.domain.model.FailureStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.simulation")
public record SimulationProperties(
        @Min(0) @Max(60000) long materialCheckDelayMs,
        @Min(0) @Max(60000) long generatingDelayMs,
        @Min(0) @Max(60000) long finalizingDelayMs,
        FailureStage failureStage
) {
}
