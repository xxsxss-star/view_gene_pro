package com.viewgene.application.asset;

import org.springframework.core.io.Resource;

public record AssetContent(Resource resource, String originalName, String mimeType, long fileSizeBytes) {
}
