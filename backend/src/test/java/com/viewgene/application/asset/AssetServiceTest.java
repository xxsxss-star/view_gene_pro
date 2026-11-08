package com.viewgene.application.asset;

import com.viewgene.common.exception.BusinessException;
import com.viewgene.config.StorageProperties;
import com.viewgene.domain.model.MaterialTemplate;
import com.viewgene.infrastructure.persistence.mapper.AssetMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.record.AssetInsertRecord;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import com.viewgene.infrastructure.storage.LocalAssetStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetServiceTest {
    private Path uploadDirectory;

    private final AuthenticatedUser user = new AuthenticatedUser(1L, "demo_user", "演示用户");
    private MaterialTemplateMapper templateMapper;
    private AssetMapper assetMapper;
    private AssetService service;

    @BeforeEach
    void setUp() throws IOException {
        uploadDirectory = Path.of("target", "test-uploads", UUID.randomUUID().toString()).toAbsolutePath();
        Files.createDirectories(uploadDirectory);
        templateMapper = mock(MaterialTemplateMapper.class);
        assetMapper = mock(AssetMapper.class);
        service = new AssetService(templateMapper, assetMapper,
                new LocalAssetStorage(new StorageProperties(uploadDirectory.toString())));
        when(templateMapper.findEnabledById("template")).thenReturn(Optional.of(template(1024)));
        doAnswer(invocation -> {
            AssetInsertRecord record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        }).when(assetMapper).insert(any(AssetInsertRecord.class));
    }

    @AfterEach
    void cleanUp() throws IOException {
        // 测试目录固定在 target 下，按子项优先顺序清理，兼容 Windows 文件系统。
        if (!Files.exists(uploadDirectory)) return;
        try (Stream<Path> paths = Files.walk(uploadDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    static Stream<MockMultipartFile> validImages() {
        return Stream.of(
                new MockMultipartFile("file", "photo.jpg", "text/plain",
                        new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01, 0x02}),
                new MockMultipartFile("file", "photo.png", "application/octet-stream",
                        new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01})
        );
    }

    @ParameterizedTest
    @MethodSource("validImages")
    void storesJpegAndPngByTheirSignatures(MockMultipartFile file) throws IOException {
        AssetDto result = service.upload(user, "template", file);

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.mimeType()).startsWith("image/");
        assertThat(result.contentUrl()).isEqualTo("/api/v1/assets/101/content");
        assertThat(storedFiles()).hasSize(1);
        verify(assetMapper).insert(any(AssetInsertRecord.class));
    }

    @Test
    void rejectsEmptyOversizedForgedAndMissingTemplate() {
        assertCode("VALIDATION_ERROR", () -> service.upload(user, "template",
                new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0])));

        when(templateMapper.findEnabledById("small")).thenReturn(Optional.of(template(4)));
        assertCode("FILE_TOO_LARGE", () -> service.upload(user, "small", validImages().skip(1).findFirst().orElseThrow()));

        assertCode("UNSUPPORTED_MEDIA_TYPE", () -> service.upload(user, "template",
                new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not an image".getBytes())));

        when(templateMapper.findEnabledById("missing")).thenReturn(Optional.empty());
        assertCode("TEMPLATE_NOT_FOUND", () -> service.upload(user, "missing", validImages().findFirst().orElseThrow()));
    }

    @Test
    void removesStoredFileWhenMetadataInsertFails() throws IOException {
        when(assetMapper.insert(any(AssetInsertRecord.class))).thenThrow(new IllegalStateException("database failed"));

        assertThatThrownBy(() -> service.upload(user, "template", validImages().findFirst().orElseThrow()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void crossUserReadReturnsNotFoundWithoutLoadingAFile() {
        when(assetMapper.findByIdAndUserId(101L, 2L)).thenReturn(Optional.empty());
        AuthenticatedUser otherUser = new AuthenticatedUser(2L, "demo_user_b", "演示用户B");

        assertCode("ASSET_NOT_FOUND", () -> service.content(otherUser, 101L));
        verify(assetMapper).findByIdAndUserId(101L, 2L);
    }

    private MaterialTemplate template(long maxSize) {
        LocalDateTime now = LocalDateTime.now();
        return new MaterialTemplate("template", 1L, "模板", "说明", "/cover.svg", "/result.svg",
                2, List.of("image/jpeg", "image/png"), maxSize, true, 1, now, now);
    }

    private List<Path> storedFiles() throws IOException {
        try (Stream<Path> paths = Files.list(uploadDirectory)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }

    private void assertCode(String code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(code);
    }
}
