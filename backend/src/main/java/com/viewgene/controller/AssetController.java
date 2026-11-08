package com.viewgene.controller;

import com.viewgene.application.asset.AssetContent;
import com.viewgene.application.asset.AssetDto;
import com.viewgene.application.asset.AssetService;
import com.viewgene.common.api.ApiResponse;
import com.viewgene.infrastructure.security.AuthenticatedUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AssetDto>> upload(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String templateId,
            @RequestParam MultipartFile file
    ) {
        return ResponseEntity.status(201).body(ApiResponse.success(assetService.upload(user, templateId, file)));
    }

    @GetMapping("/{assetId}/content")
    public ResponseEntity<Resource> content(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long assetId
    ) {
        AssetContent content = assetService.content(user, assetId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.mimeType()))
                .contentLength(content.fileSizeBytes())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.originalName(), StandardCharsets.UTF_8).build().toString())
                .body(content.resource());
    }
}
