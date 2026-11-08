package com.viewgene.infrastructure.storage;

import com.viewgene.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class LocalAssetStorage implements AssetStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalAssetStorage.class);
    private final Path root;

    public LocalAssetStorage(StorageProperties properties) {
        this.root = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, DetectedImageType imageType) throws IOException {
        Files.createDirectories(root);
        Path temporaryDirectory = root.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        String storageKey = UUID.randomUUID() + imageType.extension();
        Path temporary = temporaryDirectory.resolve(storageKey + ".part");
        Path target = resolveSafely(storageKey);
        MessageDigest digest = sha256();
        long size = 0;
        try {
            try (InputStream input = file.getInputStream(); OutputStream output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                    size += read;
                }
            }
            // 同一文件系统优先使用原子移动，避免读取到只写入一部分的文件。
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            return new StoredFile(storageKey, size, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(target);
            throw exception;
        }
    }

    @Override
    public Resource load(String storageKey) {
        return new FileSystemResource(resolveSafely(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveSafely(storageKey));
        } catch (IOException exception) {
            LOGGER.warn("Could not remove compensated asset file, storageKey={}", storageKey);
        }
    }

    private Path resolveSafely(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root) || storageKey.contains("/") || storageKey.contains("\\")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return resolved;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
