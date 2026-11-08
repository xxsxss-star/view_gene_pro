package com.viewgene;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.viewgene.infrastructure.persistence.mapper.AppUserMapper;
import com.viewgene.infrastructure.persistence.mapper.AssetMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.mapper.TemplateCategoryMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "app.persistence.enabled=false"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = "management.health.defaults.enabled=false")
class HealthEndpointTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AppUserMapper appUserMapper;

    @MockitoBean
    AssetMapper assetMapper;

    @MockitoBean
    TemplateCategoryMapper templateCategoryMapper;

    @MockitoBean
    MaterialTemplateMapper materialTemplateMapper;

    @Test
    void healthEndpointIsPublicAndDoesNotExposeDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void demoImagesArePublicStaticResources() throws Exception {
        mockMvc.perform(get("/images/templates/travel-postcard.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));
    }
}
