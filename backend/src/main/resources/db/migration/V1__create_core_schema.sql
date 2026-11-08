CREATE TABLE app_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE template_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_category_code (code),
    CONSTRAINT chk_template_category_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE material_template (
    id VARCHAR(64) NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    result_url VARCHAR(500) NOT NULL,
    required_image_count INT UNSIGNED NOT NULL,
    accepted_mime_types JSON NOT NULL,
    max_file_size_bytes BIGINT UNSIGNED NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_template_category_enabled_sort (category_id, enabled, sort_order),
    CONSTRAINT fk_material_template_category FOREIGN KEY (category_id) REFERENCES template_category (id),
    CONSTRAINT chk_material_template_image_count CHECK (required_image_count BETWEEN 1 AND 10),
    CONSTRAINT chk_material_template_file_size CHECK (max_file_size_bytes > 0),
    CONSTRAINT chk_material_template_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE asset (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_storage_key (storage_key),
    KEY idx_asset_user_created (user_id, created_at DESC),
    CONSTRAINT fk_asset_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT chk_asset_file_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_asset_status CHECK (status IN ('READY', 'INVALID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE production_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    stage VARCHAR(32) NULL,
    result_url VARCHAR(500) NULL,
    failure_code VARCHAR(64) NULL,
    failure_reason VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    updated_at DATETIME(3) NOT NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_task_user_created (user_id, created_at DESC),
    KEY idx_task_user_status_updated (user_id, status, updated_at),
    KEY idx_task_status_created (status, created_at),
    CONSTRAINT fk_production_task_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_production_task_template FOREIGN KEY (template_id) REFERENCES material_template (id),
    CONSTRAINT chk_production_task_status CHECK (status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_production_task_stage CHECK (stage IS NULL OR stage IN ('MATERIAL_CHECK', 'GENERATING', 'FINALIZING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE production_task_asset (
    task_id BIGINT UNSIGNED NOT NULL,
    position INT UNSIGNED NOT NULL,
    asset_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (task_id, position),
    UNIQUE KEY uk_task_asset (task_id, asset_id),
    KEY idx_task_asset_asset_id (asset_id),
    CONSTRAINT fk_task_asset_task FOREIGN KEY (task_id) REFERENCES production_task (id),
    CONSTRAINT fk_task_asset_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT chk_task_asset_position CHECK (position BETWEEN 1 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
