package com.viewgene.infrastructure.storage;

public enum DetectedImageType {
    JPEG("image/jpeg", ".jpg"),
    PNG("image/png", ".png");

    private final String mimeType;
    private final String extension;

    DetectedImageType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() { return mimeType; }
    public String extension() { return extension; }
}
