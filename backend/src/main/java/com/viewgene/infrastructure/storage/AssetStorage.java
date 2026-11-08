package com.viewgene.infrastructure.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AssetStorage {
    StoredFile store(MultipartFile file, DetectedImageType imageType) throws IOException;

    Resource load(String storageKey);

    void delete(String storageKey);
}
