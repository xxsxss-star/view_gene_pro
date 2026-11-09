package com.viewgene;

import com.viewgene.domain.model.UserStatus;
import com.viewgene.infrastructure.persistence.mapper.AppUserMapper;
import com.viewgene.infrastructure.persistence.mapper.MaterialTemplateMapper;
import com.viewgene.infrastructure.persistence.mapper.TemplateCategoryMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class DatabaseFoundationIntegrationTest {
    private static final List<String> CORE_TABLES = List.of(
            "app_user",
            "template_category",
            "material_template",
            "asset",
            "production_task",
            "production_task_asset"
    );

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AppUserMapper appUserMapper;

    @Autowired
    TemplateCategoryMapper categoryMapper;

    @Autowired
    MaterialTemplateMapper templateMapper;

    @Test
    void migrationsCreateAllCoreTablesIndexesAndConstraints() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('app_user', 'template_category', 'material_template', 'asset',
                                     'production_task', 'production_task_asset')
                """, Integer.class);
        assertThat(tableCount).isEqualTo(CORE_TABLES.size());

        List<String> indexNames = jdbcTemplate.queryForList("""
                SELECT DISTINCT index_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                """, String.class);
        assertThat(indexNames).contains(
                "uk_app_user_username",
                "uk_template_category_code",
                "idx_template_category_enabled_sort",
                "uk_asset_storage_key",
                "idx_asset_user_created",
                "idx_task_user_created",
                "idx_task_user_status_updated",
                "idx_task_status_created",
                "uk_task_asset",
                "idx_task_asset_asset_id"
        );

        List<String> constraintNames = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                """, String.class);
        assertThat(constraintNames).contains(
                "chk_app_user_status",
                "chk_material_template_image_count",
                "fk_material_template_category",
                "fk_asset_user",
                "fk_production_task_user",
                "fk_production_task_template",
                "fk_task_asset_task",
                "fk_task_asset_asset"
        );
    }

    @Test
    void mappersReadSeededUsersCategoriesAndTemplates() {
        var user = appUserMapper.findByUsername("demo_user").orElseThrow();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(new BCryptPasswordEncoder().matches("demo123", user.passwordHash())).isTrue();

        assertThat(categoryMapper.findAllEnabled())
                .extracting("code")
                .containsExactly("TRAVEL", "SOCIAL", "PETS", "WEDDING", "HOLIDAY", "PORTRAIT");

        var templates = templateMapper.findAllEnabled();
        assertThat(templates).hasSize(8);
        assertThat(templates).extracting("requiredImageCount").contains(1, 2, 3);
        assertThat(templates.get(0).acceptedMimeTypes()).containsExactly("image/jpeg", "image/png");
        assertThat(templateMapper.findAllEnabledByCategoryCode("TRAVEL")).hasSize(2);
        assertThat(templateMapper.findEnabledById("social-story")).isPresent();
    }

    @Test
    void rerunningFlywayDoesNotDuplicateSeedData() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM material_template", Integer.class)).isEqualTo(8);
    }
}
