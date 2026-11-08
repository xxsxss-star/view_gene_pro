package com.viewgene.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class ImageSignatureDetector {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private ImageSignatureDetector() {
    }

    public static Optional<DetectedImageType> detect(MultipartFile file) throws IOException {
        byte[] header = new byte[8];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.read(header);
        }
        if (read >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) {
            return Optional.of(DetectedImageType.JPEG);
        }
        if (read == PNG_SIGNATURE.length) {
            for (int index = 0; index < PNG_SIGNATURE.length; index++) {
                if (header[index] != PNG_SIGNATURE[index]) return Optional.empty();
            }
            return Optional.of(DetectedImageType.PNG);
        }
        return Optional.empty();
    }
}
