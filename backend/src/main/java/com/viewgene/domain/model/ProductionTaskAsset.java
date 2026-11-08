package com.viewgene.domain.model;

public record ProductionTaskAsset(
        Long taskId,
        Integer position,
        Long assetId
) {
}
