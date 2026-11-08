package com.viewgene.infrastructure.storage;

public record StoredFile(String storageKey, long size, String sha256) {
}
