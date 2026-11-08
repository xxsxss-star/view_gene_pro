package com.viewgene.controller;

import com.viewgene.application.template.TemplateDetailDto;
import com.viewgene.application.template.TemplateListDto;
import com.viewgene.application.template.TemplateQueryService;
import com.viewgene.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private final TemplateQueryService templateQueryService;

    public TemplateController(TemplateQueryService templateQueryService) {
        this.templateQueryService = templateQueryService;
    }

    @GetMapping
    public ApiResponse<TemplateListDto> list(@RequestParam(required = false) String categoryCode) {
        return ApiResponse.success(templateQueryService.list(categoryCode));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateDetailDto> detail(@PathVariable String templateId) {
        return ApiResponse.success(templateQueryService.detail(templateId));
    }
}
