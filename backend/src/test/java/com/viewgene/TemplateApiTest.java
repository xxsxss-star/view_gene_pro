package com.viewgene;

import com.viewgene.domain.model.MaterialTemplate;
import com.viewgene.domain.model.TemplateCategory;
import com.viewgene.infrastructure.persistence.mapper.AppUserMapper;
import com.viewgene.infrastructure.persistence.mapper.AssetMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.mapper.TemplateCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "app.persistence.enabled=false",
        "app.jwt.secret=stage-four-test-secret-must-be-at-least-32-bytes"
})
@AutoConfigureMockMvc
class TemplateApiTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AppUserMapper appUserMapper;
    @MockitoBean AssetMapper assetMapper;
    @MockitoBean TemplateCategoryMapper categoryMapper;
    @MockitoBean MaterialTemplateMapper templateMapper;

    private TemplateCategory travel;
    private TemplateCategory social;
    private MaterialTemplate postcard;
    private MaterialTemplate collage;
    private MaterialTemplate story;

    @BeforeEach
    void setUp() {
        travel = new TemplateCategory(1L, "TRAVEL", "旅行纪念", 10, true);
        social = new TemplateCategory(2L, "SOCIAL", "社交分享", 20, true);
        postcard = template("travel-postcard", 1L, "旅行明信片", 1, 10);
        collage = template("travel-collage", 1L, "双图旅行拼贴", 2, 20);
        story = template("social-story", 2L, "三图社交故事", 3, 10);
        when(categoryMapper.findAllEnabled()).thenReturn(List.of(travel, social));
        when(templateMapper.findAllEnabled()).thenReturn(List.of(postcard, collage, story));
        when(templateMapper.findAllEnabledByCategoryCode("TRAVEL")).thenReturn(List.of(postcard, collage));
        when(templateMapper.findAllEnabledByCategoryCode("UNKNOWN")).thenReturn(List.of());
        when(templateMapper.findEnabledById("travel-postcard")).thenReturn(Optional.of(postcard));
        when(templateMapper.findEnabledById("missing")).thenReturn(Optional.empty());
    }

    @Test
    void publicListReturnsCategoriesAndMapperOrder() throws Exception {
        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value("travel-postcard"))
                .andExpect(jsonPath("$.data.items[1].id").value("travel-collage"))
                .andExpect(jsonPath("$.data.items[2].id").value("social-story"))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void categoryFilterUsesDatabaseQueryAndUnknownCategoryIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/templates").param("categoryCode", "TRAVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2));
        verify(templateMapper).findAllEnabledByCategoryCode("TRAVEL");

        mockMvc.perform(get("/api/v1/templates").param("categoryCode", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void detailReturnsRequirementsAndMissingTemplateIs404() throws Exception {
        mockMvc.perform(get("/api/v1/templates/travel-postcard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiredImageCount").value(1))
                .andExpect(jsonPath("$.data.acceptedMimeTypes[0]").value("image/jpeg"))
                .andExpect(jsonPath("$.data.maxFileSizeBytes").value(10485760));

        mockMvc.perform(get("/api/v1/templates/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_NOT_FOUND"));
    }

    private MaterialTemplate template(String id, long categoryId, String title, int imageCount, int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        return new MaterialTemplate(
                id, categoryId, title, "模板说明", "/images/templates/" + id + ".svg",
                "/images/results/" + id + "-result.svg", imageCount, List.of("image/jpeg", "image/png"),
                10485760L, true, sortOrder, now, now
        );
    }
}
